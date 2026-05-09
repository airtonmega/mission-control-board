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

import base64
import io

from app.models.openai_schema import (
    InterviewEvalRawResponse,
    INTERVIEW_EVAL_JSON_SCHEMA,
    OpenAIRawResponse,
    RESPONSE_JSON_SCHEMA,
)
from app.models.schemas import (
    AnalyzeAudioResponse,
    AnalyzeImageResponse,
    AnalyzeTextRequest,
    AnalyzeTextResponse,
    InterviewEvaluateResponse,
)

logger = logging.getLogger(__name__)

_PROMPTS_DIR = Path(__file__).parent.parent.parent / "prompts"
_PROMPT_FILE = _PROMPTS_DIR / "system_live_assistant.md"

_IMAGE_EXTRA_INSTRUCTION = (
    "\n\nAnálise de imagem: o usuário enviou uma imagem. "
    "Além dos campos obrigatórios do JSON, inclua também o campo `detected_text` "
    "com qualquer texto visível na imagem (código, labels, texto UI, etc.). "
    "Se não houver texto visível, use uma string vazia para `detected_text`."
)

_EVAL_PROMPT_FILE = _PROMPTS_DIR / "system_interview_evaluator.md"


def _load_eval_prompt() -> str:
    if not _EVAL_PROMPT_FILE.exists():
        raise FileNotFoundError(f"Eval prompt not found: {_EVAL_PROMPT_FILE}")
    return _EVAL_PROMPT_FILE.read_text(encoding="utf-8")


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
        eval_prompt: str,
    ) -> None:
        self._client = AsyncOpenAI(api_key=api_key, timeout=timeout_seconds)
        self._model = model
        self._max_repair = max_repair_attempts
        self._system_prompt = system_prompt
        self._eval_prompt = eval_prompt

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

    async def analyze_audio(
        self, session_id: str, audio_bytes: bytes, content_type: str
    ) -> AnalyzeAudioResponse:
        wall_start = time.perf_counter()

        transcription = await self._transcribe_audio(audio_bytes, content_type)

        repair_triggered = False
        raw_dict = await self._call_openai(transcription, None)

        try:
            validated = OpenAIRawResponse.model_validate(raw_dict)
        except ValidationError as first_err:
            if self._max_repair < 1:
                raise AIResponseError(
                    user_message="A IA retornou uma resposta inválida para o áudio. Tente novamente.",
                    technical_detail=str(first_err),
                ) from first_err

            logger.warning(
                "openai_validate_audio_failed — triggering repair session_id=%s errors=%d",
                session_id,
                len(first_err.errors()),
            )
            repair_triggered = True
            repaired_dict = await self._call_repair(transcription, raw_dict, first_err)

            try:
                validated = OpenAIRawResponse.model_validate(repaired_dict)
            except ValidationError as second_err:
                raise AIResponseError(
                    user_message="A IA não conseguiu corrigir a resposta de áudio. Tente novamente.",
                    technical_detail=f"audio_repair_also_failed: {second_err}",
                ) from second_err

        elapsed_ms = int((time.perf_counter() - wall_start) * 1000)

        logger.info(
            "openai_analyze_audio session_id=%s latency_ms=%d model=%s repair=%s",
            session_id,
            elapsed_ms,
            self._model,
            repair_triggered,
        )

        return AnalyzeAudioResponse(
            session_id=session_id,
            transcription=transcription,
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

    async def _transcribe_audio(self, audio_bytes: bytes, content_type: str) -> str:
        audio_file = ("recording.m4a", io.BytesIO(audio_bytes), content_type)
        try:
            response = await self._client.audio.transcriptions.create(
                model="whisper-1",
                file=audio_file,
                language="pt",
            )
            return response.text
        except APITimeoutError as exc:
            raise AIServiceUnavailableError(
                user_message="O serviço de transcrição demorou demais. Tente novamente.",
                technical_detail=str(exc),
            ) from exc
        except APIConnectionError as exc:
            raise AIServiceUnavailableError(
                user_message="Não foi possível conectar ao serviço de transcrição.",
                technical_detail=str(exc),
            ) from exc
        except APIStatusError as exc:
            raise AIServiceUnavailableError(
                user_message=f"Serviço de transcrição retornou erro ({exc.status_code}).",
                technical_detail=str(exc),
            ) from exc
        except Exception as exc:
            raise AIResponseError(
                user_message="Erro ao transcrever o áudio. Verifique o formato e tente novamente.",
                technical_detail=f"transcription_error: {exc}",
            ) from exc

    async def analyze_image(
        self, session_id: str, image_bytes: bytes, content_type: str
    ) -> AnalyzeImageResponse:
        wall_start = time.perf_counter()
        repair_triggered = False

        raw_dict = await self._call_vision(image_bytes, content_type)
        detected_text: str = raw_dict.pop("detected_text", "")

        try:
            validated = OpenAIRawResponse.model_validate(raw_dict)
        except ValidationError as first_err:
            if self._max_repair < 1:
                raise AIResponseError(
                    user_message="A IA retornou uma resposta inválida para a imagem. Tente novamente.",
                    technical_detail=str(first_err),
                ) from first_err

            logger.warning(
                "openai_validate_image_failed — triggering repair session_id=%s errors=%d",
                session_id,
                len(first_err.errors()),
            )
            repair_triggered = True
            repaired_dict = await self._call_repair_image(raw_dict, first_err)
            detected_text = repaired_dict.pop("detected_text", detected_text)

            try:
                validated = OpenAIRawResponse.model_validate(repaired_dict)
            except ValidationError as second_err:
                raise AIResponseError(
                    user_message="A IA não conseguiu corrigir a resposta de imagem. Tente novamente.",
                    technical_detail=f"image_repair_also_failed: {second_err}",
                ) from second_err

        elapsed_ms = int((time.perf_counter() - wall_start) * 1000)

        logger.info(
            "openai_analyze_image session_id=%s latency_ms=%d model=%s repair=%s",
            session_id,
            elapsed_ms,
            self._model,
            repair_triggered,
        )

        return AnalyzeImageResponse(
            session_id=session_id,
            detected_text=detected_text,
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

    async def _call_vision(self, image_bytes: bytes, content_type: str) -> dict:
        b64 = base64.b64encode(image_bytes).decode("utf-8")
        image_system_prompt = self._system_prompt + _IMAGE_EXTRA_INSTRUCTION
        messages: list[dict] = [
            {"role": "system", "content": image_system_prompt},
            {
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:{content_type};base64,{b64}",
                            "detail": "high",
                        },
                    },
                    {
                        "type": "text",
                        "text": "Analise esta imagem e responda no formato JSON obrigatório.",
                    },
                ],
            },
        ]
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
                user_message="O serviço de IA demorou demais para processar a imagem.",
                technical_detail=str(exc),
            ) from exc
        except APIConnectionError as exc:
            raise AIServiceUnavailableError(
                user_message="Não foi possível conectar ao serviço de IA.",
                technical_detail=str(exc),
            ) from exc
        except APIStatusError as exc:
            raise AIServiceUnavailableError(
                user_message=f"Serviço de IA retornou erro ({exc.status_code}).",
                technical_detail=str(exc),
            ) from exc

        content = completion.choices[0].message.content or "{}"
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise AIResponseError(
                user_message="A IA retornou resposta malformada para a imagem.",
                technical_detail=f"image_json_decode_error: {exc} | content={content[:200]}",
            ) from exc

    async def _call_repair_image(self, bad_dict: dict, errors: ValidationError) -> dict:
        error_lines = "\n".join(
            f"  - Campo '{e['loc'][-1] if e['loc'] else '?'}': {e['msg']}"
            for e in errors.errors()
        )
        repair_prompt = (
            f"Sua resposta anterior para a análise de imagem não estava no formato correto.\n\n"
            f"Sua resposta com erros:\n{json.dumps(bad_dict, ensure_ascii=False, indent=2)}\n\n"
            f"Erros de validação encontrados:\n{error_lines}\n\n"
            f"Schema JSON obrigatório:\n{json.dumps(RESPONSE_JSON_SCHEMA, ensure_ascii=False, indent=2)}\n\n"
            f"Corrija os campos indicados e retorne APENAS o JSON corrigido, sem nenhum texto adicional."
        )
        messages: list[dict] = [
            {"role": "system", "content": self._system_prompt + _IMAGE_EXTRA_INSTRUCTION},
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
                user_message="Falha ao tentar corrigir resposta de imagem.",
                technical_detail=str(exc),
            ) from exc

        content = completion.choices[0].message.content or "{}"
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise AIResponseError(
                user_message="A IA não produziu JSON válido após correção de imagem.",
                technical_detail=f"image_repair_json_decode_error: {exc}",
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

    async def evaluate_interview(
        self,
        session_id: str,
        area: str,
        level: str,
        question: str,
        answer: str,
    ) -> InterviewEvalRawResponse:
        wall_start = time.perf_counter()
        repair_triggered = False

        raw_dict = await self._call_eval(area, level, question, answer)

        try:
            validated = InterviewEvalRawResponse.model_validate(raw_dict)
        except ValidationError as first_err:
            if self._max_repair < 1:
                raise AIResponseError(
                    user_message="A IA retornou uma avaliação em formato inválido. Tente novamente.",
                    technical_detail=str(first_err),
                ) from first_err
            logger.warning(
                "openai_validate_eval_failed — triggering repair session_id=%s errors=%d",
                session_id,
                len(first_err.errors()),
            )
            repair_triggered = True
            repaired_dict = await self._call_eval_repair(area, level, question, answer, raw_dict, first_err)
            try:
                validated = InterviewEvalRawResponse.model_validate(repaired_dict)
            except ValidationError as second_err:
                raise AIResponseError(
                    user_message="A IA não conseguiu corrigir a avaliação. Tente novamente.",
                    technical_detail=f"eval_repair_also_failed: {second_err}",
                ) from second_err

        elapsed_ms = int((time.perf_counter() - wall_start) * 1000)
        logger.info(
            "openai_evaluate_interview session_id=%s latency_ms=%d model=%s area=%s level=%s repair=%s",
            session_id, elapsed_ms, self._model, area, level, repair_triggered,
        )
        return validated

    async def _call_eval(self, area: str, level: str, question: str, answer: str) -> dict:
        user_content = (
            f"Área: {area}\nNível: {level}\n\n"
            f"Pergunta: {question}\n\nResposta do candidato: {answer}"
        )
        messages: list[dict] = [
            {"role": "system", "content": self._eval_prompt},
            {"role": "user", "content": user_content},
        ]
        try:
            completion = await self._client.chat.completions.create(
                model=self._model,
                messages=messages,
                response_format={"type": "json_object"},
                temperature=0.3,
                max_tokens=2000,
            )
        except APITimeoutError as exc:
            raise AIServiceUnavailableError(
                user_message="O serviço de avaliação demorou demais. Tente novamente.",
                technical_detail=str(exc),
            ) from exc
        except APIConnectionError as exc:
            raise AIServiceUnavailableError(
                user_message="Não foi possível conectar ao serviço de avaliação.",
                technical_detail=str(exc),
            ) from exc
        except APIStatusError as exc:
            raise AIServiceUnavailableError(
                user_message=f"Serviço de avaliação retornou erro ({exc.status_code}).",
                technical_detail=str(exc),
            ) from exc
        content = completion.choices[0].message.content or "{}"
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise AIResponseError(
                user_message="A IA retornou avaliação malformada. Tente novamente.",
                technical_detail=f"eval_json_decode_error: {exc} | content={content[:200]}",
            ) from exc

    async def _call_eval_repair(
        self, area: str, level: str, question: str, answer: str, bad_dict: dict, errors: ValidationError
    ) -> dict:
        error_lines = "\n".join(
            f"  - Campo '{e['loc'][-1] if e['loc'] else '?'}': {e['msg']}"
            for e in errors.errors()
        )
        repair_prompt = (
            f"Sua resposta anterior para a avaliação não estava no formato correto.\n\n"
            f"Área: {area} | Nível: {level}\n"
            f"Pergunta: {question}\n"
            f"Resposta avaliada: {answer}\n\n"
            f"Sua resposta com erros:\n{json.dumps(bad_dict, ensure_ascii=False, indent=2)}\n\n"
            f"Erros de validação:\n{error_lines}\n\n"
            f"Schema JSON obrigatório:\n{json.dumps(INTERVIEW_EVAL_JSON_SCHEMA, ensure_ascii=False, indent=2)}\n\n"
            f"Corrija os campos indicados e retorne APENAS o JSON corrigido."
        )
        messages: list[dict] = [
            {"role": "system", "content": self._eval_prompt},
            {"role": "user", "content": repair_prompt},
        ]
        try:
            completion = await self._client.chat.completions.create(
                model=self._model,
                messages=messages,
                response_format={"type": "json_object"},
                temperature=0.3,
                max_tokens=2000,
            )
        except (APITimeoutError, APIConnectionError, APIStatusError) as exc:
            raise AIServiceUnavailableError(
                user_message="Falha ao tentar corrigir a avaliação.",
                technical_detail=str(exc),
            ) from exc
        content = completion.choices[0].message.content or "{}"
        try:
            return json.loads(content)
        except json.JSONDecodeError as exc:
            raise AIResponseError(
                user_message="A IA não produziu JSON válido após correção da avaliação.",
                technical_detail=f"eval_repair_json_decode_error: {exc}",
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
            eval_prompt=_load_eval_prompt(),
        )
    return _instance


def reset_openai_service() -> None:
    """Test helper — resets the singleton so tests can inject custom instances."""
    global _instance
    _instance = None
