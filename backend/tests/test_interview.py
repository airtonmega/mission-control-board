"""Tests for /interview endpoints — Phase 5."""
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services.openai_service import AIResponseError


@pytest.mark.anyio
async def test_interview_start_mock(client):
    """POST /interview/start returns all required fields."""
    response = await client.post(
        "/interview/start",
        json={"session_id": "iv-start-001", "area": "Android", "level": "Pleno"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == "iv-start-001"
    assert "interview_id" in data
    assert "first_question" in data
    assert len(data["first_question"]) > 0
    assert data["total_questions"] == 5
    assert data["area"] == "Android"
    assert data["level"] == "Pleno"


@pytest.mark.anyio
async def test_interview_start_different_areas(client):
    """Different areas return different first questions."""
    resp_android = await client.post(
        "/interview/start",
        json={"session_id": "iv-area-001", "area": "Android", "level": "Pleno"},
    )
    resp_backend = await client.post(
        "/interview/start",
        json={"session_id": "iv-area-002", "area": "Backend", "level": "Pleno"},
    )
    assert resp_android.status_code == 200
    assert resp_backend.status_code == 200
    q_android = resp_android.json()["first_question"]
    q_backend = resp_backend.json()["first_question"]
    assert q_android != q_backend


@pytest.mark.anyio
async def test_interview_evaluate_mock_progress(client):
    """Evaluating question 1 of 5 returns progress fields, completed=False, next_question set."""
    response = await client.post(
        "/interview/evaluate",
        json={
            "session_id": "iv-eval-001",
            "interview_id": "iv-id-001",
            "question": "O que são Kotlin Coroutines?",
            "answer": "Coroutines são unidades de computação suspensáveis que permitem código assíncrono.",
            "question_number": 1,
            "total_questions": 5,
            "area": "Android",
            "level": "Pleno",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert "score" in data
    assert "accuracy" in data
    assert "clarity" in data
    assert "depth" in data
    assert data["completed"] is False
    assert data["next_question"] is not None
    assert len(data["next_question"]) > 0


@pytest.mark.anyio
async def test_interview_evaluate_mock_last_question(client):
    """Evaluating question 5 of 5 returns completed=True and next_question=None."""
    response = await client.post(
        "/interview/evaluate",
        json={
            "session_id": "iv-eval-002",
            "interview_id": "iv-id-002",
            "question": "Explique o ciclo de vida do Jetpack Compose.",
            "answer": "O Compose usa recomposição quando o estado muda.",
            "question_number": 5,
            "total_questions": 5,
            "area": "Android",
            "level": "Pleno",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["completed"] is True
    assert data["next_question"] is None


@pytest.mark.anyio
async def test_interview_evaluate_all_fields_present(client):
    """Response contains strengths, weaknesses, and improved_answer — all non-empty."""
    response = await client.post(
        "/interview/evaluate",
        json={
            "session_id": "iv-eval-003",
            "interview_id": "iv-id-003",
            "question": "Explique MVVM no Android.",
            "answer": "MVVM separa UI de lógica de negócio usando ViewModel.",
            "question_number": 2,
            "total_questions": 5,
            "area": "Android",
            "level": "Pleno",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data["strengths"], list) and len(data["strengths"]) > 0
    assert isinstance(data["weaknesses"], list) and len(data["weaknesses"]) > 0
    assert isinstance(data["improved_answer"], str) and len(data["improved_answer"]) > 0


@pytest.mark.anyio
async def test_interview_evaluate_score_range(client):
    """Score returned is between 0 and 10 inclusive."""
    response = await client.post(
        "/interview/evaluate",
        json={
            "session_id": "iv-eval-004",
            "interview_id": "iv-id-004",
            "question": "Qual a diferença entre StateFlow e SharedFlow?",
            "answer": "StateFlow tem sempre um valor inicial e representa estado; SharedFlow é mais flexível.",
            "question_number": 3,
            "total_questions": 5,
            "area": "Android",
            "level": "Pleno",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["score"] <= 10.0


@pytest.mark.anyio
async def test_interview_evaluate_real_mode_returns_structured_error(client):
    """When real AI mode raises AIResponseError, returns 422 with AI_RESPONSE_INVALID."""
    err = AIResponseError(
        user_message="A IA retornou uma avaliação em formato inválido. Tente novamente.",
        technical_detail="missing score field",
    )

    mock_svc = MagicMock()
    mock_svc.evaluate_interview = AsyncMock(side_effect=err)

    with patch("app.routers.interview._should_use_mock", return_value=False), \
         patch("app.routers.interview.get_openai_service", return_value=mock_svc):
        response = await client.post(
            "/interview/evaluate",
            json={
                "session_id": "iv-err-001",
                "interview_id": "iv-id-err",
                "question": "Pergunta de teste",
                "answer": "Resposta de teste",
                "question_number": 1,
                "total_questions": 5,
                "area": "Android",
                "level": "Pleno",
            },
        )

    assert response.status_code == 422
    detail = response.json()["detail"]
    assert detail["error_code"] == "AI_RESPONSE_INVALID"
    assert detail["session_id"] == "iv-err-001"
    assert detail["repair_attempted"] is True
