from fastapi import APIRouter, HTTPException
from datetime import datetime, timezone
from app.models.schemas import SessionReport

router = APIRouter(prefix="/reports", tags=["reports"])


@router.get("/session/{session_id}", response_model=SessionReport)
async def get_session_report(session_id: str):
    if len(session_id) < 5:
        raise HTTPException(status_code=404, detail="Session not found")
    return SessionReport(
        session_id=session_id,
        total_queries=7,
        topics_covered=[
            "Kotlin Coroutines",
            "Jetpack Compose",
            "Clean Architecture",
            "MVVM Pattern",
        ],
        average_confidence=0.84,
        duration_seconds=1440,
        created_at=datetime.now(timezone.utc).isoformat(),
        highlights=[
            "Forte compreensão de Coroutines e concorrência",
            "Conhecimento sólido de arquitetura MVVM",
            "Oportunidade de melhoria: tratamento de erros com Flow",
        ],
    )
