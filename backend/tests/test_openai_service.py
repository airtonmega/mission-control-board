"""Unit tests for OpenAIService.

All tests mock the AsyncOpenAI client — no real API calls are made.
"""
import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from pydantic import ValidationError

from app.models.openai_schema import OpenAIRawResponse
from app.models.schemas import AnalyzeTextRequest
from app.services.openai_service import (
    AIResponseError,
    AIServiceUnavailableError,
    OpenAIService,
)

# ── Fixtures ──────────────────────────────────────────────────────────────────

VALID_PAYLOAD = {
    "detected_theme": "Kotlin — Coroutines & Concorrência",
    "quick_tip": "Use supervisorScope para isolar falhas.",
    "short_answer": "Coroutines são unidades de computação suspensas.",
    "interview_answer": "Em uma entrevista abordaria três pontos: conceito, uso e trade-offs.",
    "complete_answer": "## Kotlin Coroutines\n\nExplicação completa...",
    "common_errors": ["Usar GlobalScope em produção", "Não tratar exceções com CoroutineExceptionHandler"],
    "study_suggestions": ["kotlinlang.org/docs/coroutines", "Livro: Kotlin Coroutines Deep Dive"],
    "confidence_score": 0.95,
}

MISSING_FIELD_PAYLOAD = {k: v for k, v in VALID_PAYLOAD.items() if k != "quick_tip"}

EMPTY_LISTS_PAYLOAD = {**VALID_PAYLOAD, "common_errors": [], "study_suggestions": []}


def _make_service(system_prompt: str = "test prompt", max_repair: int = 1) -> OpenAIService:
    return OpenAIService(
        api_key="sk-test",
        model="gpt-4o-mini",
        timeout_seconds=30.0,
        max_repair_attempts=max_repair,
        system_prompt=system_prompt,
        eval_prompt="test eval prompt",
    )


def _make_request() -> AnalyzeTextRequest:
    return AnalyzeTextRequest(session_id="test-001", question="O que são Kotlin Coroutines?")


def _mock_completion(payload: dict) -> MagicMock:
    msg = MagicMock()
    msg.content = json.dumps(payload, ensure_ascii=False)
    choice = MagicMock()
    choice.message = msg
    completion = MagicMock()
    completion.choices = [choice]
    return completion


# ── OpenAIRawResponse validation ──────────────────────────────────────────────

def test_raw_response_valid():
    model = OpenAIRawResponse.model_validate(VALID_PAYLOAD)
    assert model.detected_theme == "Kotlin — Coroutines & Concorrência"
    assert model.confidence_score == 0.95
    assert len(model.common_errors) == 2


def test_raw_response_missing_field_raises():
    with pytest.raises(ValidationError) as exc_info:
        OpenAIRawResponse.model_validate(MISSING_FIELD_PAYLOAD)
    errors = exc_info.value.errors()
    assert any(e["loc"] == ("quick_tip",) for e in errors)


def test_raw_response_empty_lists_raises():
    with pytest.raises(ValidationError):
        OpenAIRawResponse.model_validate(EMPTY_LISTS_PAYLOAD)


def test_raw_response_confidence_string_coerced():
    payload = {**VALID_PAYLOAD, "confidence_score": "0.88"}
    model = OpenAIRawResponse.model_validate(payload)
    assert model.confidence_score == pytest.approx(0.88)


def test_raw_response_confidence_invalid_coerced_to_default():
    payload = {**VALID_PAYLOAD, "confidence_score": "not-a-number"}
    model = OpenAIRawResponse.model_validate(payload)
    assert model.confidence_score == pytest.approx(0.7)


# ── OpenAIService happy path ──────────────────────────────────────────────────

@pytest.mark.anyio
async def test_analyze_text_success():
    svc = _make_service()
    req = _make_request()
    mock_create = AsyncMock(return_value=_mock_completion(VALID_PAYLOAD))

    with patch("app.services.openai_service.AsyncOpenAI") as mock_cls:
        mock_cls.return_value.chat.completions.create = mock_create
        svc._client = mock_cls.return_value
        result = await svc.analyze_text(req)

    assert result.detected_theme == VALID_PAYLOAD["detected_theme"]
    assert result.processing_time_ms >= 0
    assert mock_create.call_count == 1


@pytest.mark.anyio
async def test_analyze_text_latency_recorded():
    svc = _make_service()
    req = _make_request()
    mock_create = AsyncMock(return_value=_mock_completion(VALID_PAYLOAD))

    with patch("app.services.openai_service.AsyncOpenAI"):
        svc._client = MagicMock()
        svc._client.chat.completions.create = mock_create
        result = await svc.analyze_text(req)

    assert result.processing_time_ms >= 0


# ── Repair flow ───────────────────────────────────────────────────────────────

@pytest.mark.anyio
async def test_repair_triggered_on_missing_field():
    """First call returns payload missing quick_tip; second (repair) call returns valid."""
    svc = _make_service(max_repair=1)
    req = _make_request()

    first = _mock_completion(MISSING_FIELD_PAYLOAD)
    second = _mock_completion(VALID_PAYLOAD)
    mock_create = AsyncMock(side_effect=[first, second])

    with patch("app.services.openai_service.AsyncOpenAI"):
        svc._client = MagicMock()
        svc._client.chat.completions.create = mock_create
        result = await svc.analyze_text(req)

    assert result.quick_tip == VALID_PAYLOAD["quick_tip"]
    assert mock_create.call_count == 2


@pytest.mark.anyio
async def test_repair_second_failure_raises_ai_response_error():
    """Both calls return invalid payload — AIResponseError must be raised."""
    svc = _make_service(max_repair=1)
    req = _make_request()

    mock_create = AsyncMock(
        side_effect=[
            _mock_completion(MISSING_FIELD_PAYLOAD),
            _mock_completion(MISSING_FIELD_PAYLOAD),
        ]
    )

    with patch("app.services.openai_service.AsyncOpenAI"):
        svc._client = MagicMock()
        svc._client.chat.completions.create = mock_create
        with pytest.raises(AIResponseError) as exc_info:
            await svc.analyze_text(req)

    assert "reparar" in exc_info.value.user_message.lower() or "corrigir" in exc_info.value.user_message.lower()
    assert mock_create.call_count == 2


@pytest.mark.anyio
async def test_no_repair_when_max_repair_zero():
    """With max_repair=0, validation failure immediately raises AIResponseError."""
    svc = _make_service(max_repair=0)
    req = _make_request()

    mock_create = AsyncMock(return_value=_mock_completion(MISSING_FIELD_PAYLOAD))

    with patch("app.services.openai_service.AsyncOpenAI"):
        svc._client = MagicMock()
        svc._client.chat.completions.create = mock_create
        with pytest.raises(AIResponseError):
            await svc.analyze_text(req)

    assert mock_create.call_count == 1


# ── Error propagation ─────────────────────────────────────────────────────────

@pytest.mark.anyio
async def test_malformed_json_raises_ai_response_error():
    """OpenAI returns a non-JSON string — AIResponseError must be raised."""
    svc = _make_service()
    req = _make_request()

    msg = MagicMock()
    msg.content = "this is not json at all"
    choice = MagicMock()
    choice.message = msg
    bad_completion = MagicMock()
    bad_completion.choices = [choice]

    mock_create = AsyncMock(return_value=bad_completion)

    with patch("app.services.openai_service.AsyncOpenAI"):
        svc._client = MagicMock()
        svc._client.chat.completions.create = mock_create
        with pytest.raises(AIResponseError) as exc_info:
            await svc.analyze_text(req)

    assert "malformada" in exc_info.value.user_message.lower()


@pytest.mark.anyio
async def test_timeout_raises_ai_service_unavailable():
    from openai import APITimeoutError as OAITimeout

    svc = _make_service()
    req = _make_request()

    with patch("app.services.openai_service.AsyncOpenAI"):
        svc._client = MagicMock()
        svc._client.chat.completions.create = AsyncMock(
            side_effect=OAITimeout(request=MagicMock())
        )
        with pytest.raises(AIServiceUnavailableError) as exc_info:
            await svc.analyze_text(req)

    assert "demorou" in exc_info.value.user_message.lower()
