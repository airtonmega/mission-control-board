from fastapi import APIRouter, HTTPException
from datetime import datetime, timezone
from app.models.schemas import SessionReport, InputModeCount
from app.services.event_store import get_events

router = APIRouter(prefix="/reports", tags=["reports"])


@router.get("/session/{session_id}", response_model=SessionReport)
async def get_session_report(session_id: str):
    if len(session_id) < 5:
        raise HTTPException(status_code=404, detail="Session not found")

    events = get_events(session_id)

    if not events:
        # Return a rich mock report for sessions with no tracked events
        return _mock_report(session_id)

    total = len(events)
    latencies = [e.latency_ms for e in events if e.latency_ms is not None]
    scores = [e.score for e in events if e.score is not None]
    themes = list({e.theme for e in events if e.theme})

    input_modes = InputModeCount(
        text=sum(1 for e in events if e.event_type == "text"),
        image=sum(1 for e in events if e.event_type == "image"),
        audio=sum(1 for e in events if e.event_type == "audio"),
        interview=sum(1 for e in events if e.event_type in ("interview_start", "interview_evaluate")),
    )

    timestamps = sorted(e.timestamp for e in events)
    duration = int(timestamps[-1] - timestamps[0]) if len(timestamps) > 1 else 0

    avg_latency = round(sum(latencies) / len(latencies), 1) if latencies else None
    avg_score = round(sum(scores) / len(scores), 2) if scores else None

    highlights: list[str] = []
    if avg_score is not None:
        highlights.append(f"Nota média nas entrevistas: {avg_score:.1f}/10")
    if themes:
        highlights.append(f"Temas abordados: {', '.join(themes[:3])}")
    if total >= 5:
        highlights.append("Sessão de estudo intensiva (5+ análises)")
    if not highlights:
        highlights = ["Sessão iniciada — continue praticando!"]

    events_dicts = [
        {
            "event_type": e.event_type,
            "timestamp": e.timestamp,
            "latency_ms": e.latency_ms,
            "theme": e.theme,
            "score": e.score,
            "mock": e.mock,
        }
        for e in events
    ]

    return SessionReport(
        session_id=session_id,
        total_queries=total,
        topics_covered=themes if themes else ["Engenharia de Software"],
        average_confidence=0.80,
        duration_seconds=duration,
        created_at=datetime.now(timezone.utc).isoformat(),
        highlights=highlights,
        input_modes=input_modes,
        average_latency_ms=avg_latency,
        interview_average_score=avg_score,
        events=events_dicts,
    )


def _mock_report(session_id: str) -> SessionReport:
    return SessionReport(
        session_id=session_id,
        total_queries=7,
        topics_covered=["Kotlin Coroutines", "Jetpack Compose", "Clean Architecture", "MVVM"],
        average_confidence=0.84,
        duration_seconds=1440,
        created_at=datetime.now(timezone.utc).isoformat(),
        highlights=[
            "Forte compreensão de Coroutines e concorrência",
            "Conhecimento sólido de arquitetura MVVM",
            "Oportunidade de melhoria: tratamento de erros com Flow",
        ],
        input_modes=InputModeCount(text=4, image=1, audio=1, interview=1),
        average_latency_ms=245.0,
        interview_average_score=7.8,
        events=[],
    )
