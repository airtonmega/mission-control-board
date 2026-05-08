"""Isolated OpenAI integration service.

Responsibilities:
- Load system prompt from prompts/system_live_assistant.md at startup
- Call OpenAI with json_object mode and the system prompt
- Validate response with OpenAIRawResponse (Pydantic)
- On validation failure: attempt one repair call with targeted instructions
- Record latency and log structured fields
- Raise AIResponseError with user-friendly message on unrecoverable failure
"""
import json
import logging
import time
from pathlib import Path
from typing import Optional

from openai import AsyncOpenAI, APITimeoutError, APIConnectionError, APIStatusError
from pydantic import ValidationError

from app.models.openai_schema import OpenAIRawResponse, RESPONSE_JSON_SCHEMA
from app.models.schemas import AnalyzeTextRequest, AnalyzeTextResponse

logger = logging.getLogger(__name__)

_PROMPTS_DIR = Path(__file__).parent.parent.parent / "prompts"
_PROMPT_FILE = _PROMPTS_DIR / "system_live_assistant.md"


class AIResponseError(Exception):
    """Raised when OpenAI returns a response that cannot be validated."""

    def __init__(self, user_message: str, technical_detail: str = "") -> None:
        super().__init__(technical_detail or user_message)
        self.user_message = user_message
        self.technical_detail = technical_detail


class AIServiceUnavailableError(Exception):
    """Raised when OpenAI is unreachable or returns an unexpected HTTP error."""

    def __init__(self, user_message: str, technical_detail: str = "") -> None:
        super().__init__(technical_detail or user_message)
        self.user_message = user_message
        self.technical_detail = technical_detail


def _load_system_prompt() -> str:
    if not _PROMPT_FILE.exists():
        raise FileNotFoundError(f"System prompt not found: {_PROMPT_FILE}")
    return _PROMPT_FILE.read_text(encoding="utf-8")


def _build_repair_prompt(question: str, bad_response: dict, errors: ValidationError) -> str:
    error_lines = "\n".join(
        f"  - Campo '{e['loc'][-1] if e['loc'] else '?'}': {e['msg']}"
        for e in errors.errors()
    )
    return (
        f"Sua resposta anterior para a pergunta abaixo não estava no formato correto.\n\n"
        f"Pergunta original: {question}\n\n"
        f"Sua resposta com erros:\n{json.dumps(bad_response, ensure_ascii=False, indent=2)}\n\n"
        f"Erros de validação encontrados:\n{error_lines}\n\n"
        f"Schema JSON obrigatório:\n{json.dumps(RESPONSE_JSON_SCHEMA, ensure_ascii=False, indent=2)}\n\n"
        f"Corrija os campos indicados e retorne APENAS o JSON corrigido, sem nenhum texto adicional."
    )


class OpenAIService:
    """Stateless service wrapper around the OpenAI async client."""

    def __init__(
        self,
        api_key: str,
        model: str,
        timeout_seconds: float,
        max_repair_attempts: int,
        system_prompt: str,
    ) -> None:
        self._client = AsyncOpenAI(api_key=api_key, timeout=timeout_seconds)
        self._model = model
        self._max_repair = max_repair_attempts
        self._system_prompt = system_prompt

    async def analyze_text(self, req: AnalyzeTextRequest) -> AnalyzeTextResponse:
        wall_start = time.perf_counter()
        repair_triggered = False

        raw_dict = await self._call_openai(req.question, req.context)

        try:
            validated = OpenAIRawResponse.model_validate(raw_dict)
        except ValidationError as first_err:
            if self._max_repair < 1:
                raise AIResponseError(
                    user_message="A IA retornou uma resposta em formato inválido. Tente reformular a pergunta.",
                    technical_detail=str(first_err),
                ) from first_err

            logger.warning(
                "openai_validate_failed — triggering repair session_id=%s errors=%d",
                req.session_id,
                len(first_err.errors()),
            )
            repair_triggered = True
            repaired_dict = await self._call_repair(req.question, raw_dict, first_err)

            try:
                validated = OpenAIRawResponse.model_validate(repaired_dict)
            except ValidationError as second_err:
                raise AIResponseError(
                    user_message="A IA não conseguiu corrigir a resposta. Tente novamente ou reformule a pergunta.",
                    technical_detail=f"repair_also_failed: {second_err}",
                ) from second_err

        elapsed_ms = int((time.perf_counter() - wall_start) * 1000)

        logger.info(
            "openai_analyze_text session_id=%s latency_ms=%d model=%s repair=%s",
            req.session_id,
            elapsed_ms,
            self._model,
            repair_triggered,
        )

        return AnalyzeTextResponse(
            session_id=req.session_id,
            detected_theme=validated.detected_theme,
            quick_tip=validated.quick_tip,
            short_answer=validated.short_answer,
            interview_answer=validated.interview_answer,
            complete_answer=validated.complete_answer,
            common_errors=validated.common_errors,
            study_suggestions=validated.study_suggestions,
            confidence_score=validated.confidence_score,
            processing_time_ms=elapsed_ms,
            mock=False,
        )

    async def _call_openai(self, question: str, context: Optional[str]) -> dict:
        messages: list[dict] = [{"role": "system", "content": self._system_prompt}]
        if context:
            messages.append({"role": "user", "content": f"Contexto adicional: {context}"})
        messages.append({"role": "user", "content": question})

        try:
            completion = await self._client.chat.completions.create(
                model=self._model,
                messages=messages,
                response_format={"type": "json_object"},
                temperature=0.7,
                max_tokens=3000,
            )
        except APITimeoutError as exc:
            raise AIServiceUnavailableError(
                user_message="O serviço de IA demorou demais para responder. Tente novamente.",
                technical_detail=str(exc),
            ) from exc
        except APIConnectionError as exc:
            raise AIServiceUnavailableError(
                user_message="Não foi possível conectar ao serviço de IA. Verifique a conexão.",
                technical_detail=str(exc),
            ) from exc
        except APIStatusError as exc:
            raise AIServiceUnavailableError(
                user_message=f"Serviço de IA retornou erro ({exc.status_code}). Tente novamente.",
                technical_detail=str(exc),
            ) from exc

        content = completion.choices[0].message.content or "{}"
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise AIResponseError(
                user_message="A IA retornou uma resposta malformada. Tente novamente.",
                technical_detail=f"json_decode_error: {exc} | content={content[:200]}",
            ) from exc

    async def _call_repair(
        self, question: str, bad_dict: dict, errors: ValidationError
    ) -> dict:
        repair_prompt = _build_repair_prompt(question, bad_dict, errors)
        messages: list[dict] = [
            {"role": "system", "content": self._system_prompt},
            {"role": "user", "content": repair_prompt},
        ]
        try:
            completion = await self._client.chat.completions.create(
                model=self._model,
                messages=messages,
                response_format={"type": "json_object"},
                temperature=0.3,
                max_tokens=3000,
            )
        except (APITimeoutError, APIConnectionError, APIStatusError) as exc:
            raise AIServiceUnavailableError(
                user_message="Falha ao tentar corrigir resposta. Tente novamente.",
                technical_detail=str(exc),
            ) from exc

        content = completion.choices[0].message.content or "{}"
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise AIResponseError(
                user_message="A IA não conseguiu produzir JSON válido após correção.",
                technical_detail=f"repair_json_decode_error: {exc}",
            ) from exc


# ── Singleton factory ─────────────────────────────────────────────────────────

_instance: Optional[OpenAIService] = None


def get_openai_service() -> OpenAIService:
    """Returns a module-level singleton, built once from settings."""
    global _instance
    if _instance is None:
        from app.core.config import get_settings
        s = get_settings()
        _instance = OpenAIService(
            api_key=s.openai_api_key,
            model=s.openai_model,
            timeout_seconds=s.openai_timeout_seconds,
            max_repair_attempts=s.openai_max_repair_attempts,
            system_prompt=_load_system_prompt(),
        )
    return _instance


def reset_openai_service() -> None:
    """Test helper — resets the singleton so tests can inject custom instances."""
    global _instance
    _instance = None
