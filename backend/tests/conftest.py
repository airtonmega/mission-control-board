import pytest
import os
from httpx import AsyncClient, ASGITransport
from unittest.mock import AsyncMock, MagicMock, patch

os.environ.setdefault("OPENAI_API_KEY", "sk-test-key-for-testing")
os.environ.setdefault("JWT_SECRET", "test-secret-key")

from app.main import app  # noqa: E402


@pytest.fixture(autouse=True)
def mock_openai_service():
    """Patch OpenAI service globally for all tests — zero real API calls."""
    from app.models.openai_schema import InterviewEvalRawResponse
    from app.models.schemas import (
        AnalyzeTextResponse, AnalyzeImageResponse, AnalyzeAudioResponse,
    )

    svc = MagicMock()
    svc.analyze_text = AsyncMock(return_value=AnalyzeTextResponse(
        session_id="test-session",
        detected_theme="Kotlin — Coroutines & Concorrência",
        quick_tip="Use structured concurrency.",
        short_answer="Coroutines são unidades suspensáveis.",
        interview_answer="Abordo em três pontos: conceito, uso e trade-offs.",
        complete_answer="## Kotlin Coroutines\n\nExplicação completa.",
        common_errors=["Usar GlobalScope em produção", "Não tratar exceções com CoroutineExceptionHandler"],
        study_suggestions=["kotlinlang.org/docs/coroutines", "Livro: Kotlin Coroutines Deep Dive"],
        confidence_score=0.85,
        processing_time_ms=200,
    ))
    svc.analyze_image = AsyncMock(return_value=AnalyzeImageResponse(
        session_id="test-session",
        detected_text="código kotlin",
        detected_theme="Android — MVVM",
        quick_tip="Separe responsabilidades.",
        short_answer="MVVM separa View e ViewModel.",
        interview_answer="Em uma entrevista descrevo as três camadas.",
        complete_answer="## MVVM\n\nDetalhes arquiteturais.",
        common_errors=["Passar Context ao ViewModel"],
        study_suggestions=["developer.android.com/topic/architecture"],
        confidence_score=0.82,
        processing_time_ms=300,
    ))
    svc.analyze_audio = AsyncMock(return_value=AnalyzeAudioResponse(
        session_id="test-session",
        transcription="O que são Kotlin Coroutines?",
        detected_theme="Kotlin — Coroutines & Concorrência",
        quick_tip="Use structured concurrency.",
        short_answer="Coroutines são suspensáveis.",
        interview_answer="Abordo em três pontos.",
        complete_answer="## Kotlin Coroutines\n\nDetalhes.",
        common_errors=["Usar GlobalScope"],
        study_suggestions=["kotlinlang.org"],
        confidence_score=0.88,
        processing_time_ms=400,
    ))
    svc.evaluate_interview = AsyncMock(return_value=InterviewEvalRawResponse(
        score=7.5,
        accuracy=7.2,
        clarity=8.0,
        depth=7.3,
        strengths=["Boa estrutura de resposta", "Terminologia técnica correta"],
        weaknesses=["Poderia incluir exemplos de código concretos"],
        improved_answer=(
            "Uma resposta completa incluiria: (1) definição precisa do conceito, "
            "(2) exemplo de código comentado, (3) trade-offs e casos de uso reais."
        ),
    ))

    with patch("app.routers.analyze.get_openai_service", return_value=svc), \
         patch("app.routers.interview.get_openai_service", return_value=svc):
        yield svc


@pytest.fixture
async def client():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac
