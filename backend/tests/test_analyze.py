import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services.openai_service import (
    AIResponseError,
    AIServiceUnavailableError,
    reset_openai_service,
)


@pytest.mark.anyio
async def test_analyze_text_mock(client):
    response = await client.post(
        "/analyze/text",
        json={
            "session_id": "test-session-001",
            "question": "O que são Kotlin Coroutines e como funcionam?",
            "language": "pt-BR",
        },
    )
    assert response.status_code == 200
    data = response.json()

    assert "detected_theme" in data
    assert "quick_tip" in data
    assert "short_answer" in data
    assert "interview_answer" in data
    assert "complete_answer" in data
    assert isinstance(data["common_errors"], list)
    assert isinstance(data["study_suggestions"], list)
    assert 0.0 <= data["confidence_score"] <= 1.0
    assert data["mock"] is True
    assert data["processing_time_ms"] > 0


@pytest.mark.anyio
async def test_analyze_text_theme_detection(client):
    response = await client.post(
        "/analyze/text",
        json={
            "session_id": "test-session-002",
            "question": "Como usar Jetpack Compose com Material 3?",
            "language": "pt-BR",
        },
    )
    data = response.json()
    assert "compose" in data["detected_theme"].lower() or "android" in data["detected_theme"].lower()


@pytest.mark.anyio
async def test_analyze_text_validation_error(client):
    response = await client.post(
        "/analyze/text",
        json={"session_id": "x", "question": "ab"},
    )
    assert response.status_code == 422


@pytest.mark.anyio
async def test_analyze_image_mock(client):
    response = await client.post(
        "/analyze/image",
        data={"session_id": "test-session-003"},
        files={"image": ("test.jpg", b"\xff\xd8\xff\xe0fake-jpeg-bytes", "image/jpeg")},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["mock"] is True
    assert "detected_text" in data
    assert "detected_theme" in data
    assert isinstance(data["common_errors"], list)
    assert isinstance(data["study_suggestions"], list)
    assert 0.0 <= data["confidence_score"] <= 1.0


@pytest.mark.anyio
async def test_analyze_image_error_returns_structured(client):
    """When OpenAIService raises AIResponseError for image, returns 422 structured."""
    err = AIResponseError(user_message="Imagem inválida.", technical_detail="bad")

    def _mock_svc_image(exc):
        svc = MagicMock()
        svc.analyze_image = AsyncMock(side_effect=exc)
        return svc

    with patch("app.routers.analyze._should_use_mock", return_value=False), \
         patch("app.routers.analyze.get_openai_service", return_value=_mock_svc_image(err)):
        response = await client.post(
            "/analyze/image",
            data={"session_id": "err-img-001"},
            files={"image": ("test.jpg", b"fake", "image/jpeg")},
        )

    assert response.status_code == 422
    detail = response.json()["detail"]
    assert detail["error_code"] == "AI_RESPONSE_INVALID"
    assert detail["session_id"] == "err-img-001"


@pytest.mark.anyio
async def test_interview_start(client):
    response = await client.post(
        "/interview/start",
        json={"session_id": "test-session-004", "area": "Android", "level": "Pleno"},
    )
    assert response.status_code == 200
    data = response.json()
    assert "first_question" in data
    assert data["total_questions"] > 0


@pytest.mark.anyio
async def test_interview_evaluate(client):
    response = await client.post(
        "/interview/evaluate",
        json={
            "session_id": "test-session-005",
            "interview_id": "iv-001",
            "question": "O que são coroutines?",
            "answer": "São unidades de computação suspensas...",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["score"] <= 10.0
    assert "feedback" in data


@pytest.mark.anyio
async def test_session_report(client):
    response = await client.get("/reports/session/test-session-001")
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == "test-session-001"
    assert isinstance(data["topics_covered"], list)


# ── Structured error response tests ──────────────────────────────────────────
# These tests verify that errors from OpenAIService are returned as structured
# JSON (AnalyzeErrorDetail), not bare strings, so Android can parse them.


def _mock_svc_raising(exc: Exception) -> MagicMock:
    svc = MagicMock()
    svc.analyze_text = AsyncMock(side_effect=exc)
    return svc


@pytest.mark.anyio
async def test_analyze_text_ai_response_error_returns_422_structured(client):
    """When OpenAIService raises AIResponseError, endpoint returns 422 with structured detail."""
    err = AIResponseError(user_message="A IA retornou resposta inválida.", technical_detail="missing quick_tip")

    with patch("app.routers.analyze._should_use_mock", return_value=False), \
         patch("app.routers.analyze.get_openai_service", return_value=_mock_svc_raising(err)):
        response = await client.post(
            "/analyze/text",
            json={"session_id": "err-001", "question": "Pergunta de teste?"},
        )

    assert response.status_code == 422
    detail = response.json()["detail"]
    assert detail["error_code"] == "AI_RESPONSE_INVALID"
    assert detail["session_id"] == "err-001"
    assert isinstance(detail["error_message"], str) and len(detail["error_message"]) > 0
    assert detail["repair_attempted"] is True


@pytest.mark.anyio
async def test_analyze_text_service_unavailable_returns_503_structured(client):
    """When OpenAIService raises AIServiceUnavailableError, endpoint returns 503."""
    err = AIServiceUnavailableError(user_message="Serviço temporariamente indisponível.", technical_detail="conn")

    with patch("app.routers.analyze._should_use_mock", return_value=False), \
         patch("app.routers.analyze.get_openai_service", return_value=_mock_svc_raising(err)):
        response = await client.post(
            "/analyze/text",
            json={"session_id": "err-002", "question": "Pergunta de teste?"},
        )

    assert response.status_code == 503
    detail = response.json()["detail"]
    assert detail["error_code"] == "AI_SERVICE_UNAVAILABLE"
    assert detail["session_id"] == "err-002"


@pytest.mark.anyio
async def test_analyze_text_error_detail_has_required_fields(client):
    """AnalyzeErrorDetail always contains all required keys for Android to parse."""
    err = AIResponseError(user_message="Falha.", technical_detail="x")

    with patch("app.routers.analyze._should_use_mock", return_value=False), \
         patch("app.routers.analyze.get_openai_service", return_value=_mock_svc_raising(err)):
        response = await client.post(
            "/analyze/text",
            json={"session_id": "err-003", "question": "Pergunta longa o suficiente"},
        )

    detail = response.json()["detail"]
    for key in ("error_code", "error_message", "session_id", "repair_attempted"):
        assert key in detail, f"Missing key in error detail: {key}"
