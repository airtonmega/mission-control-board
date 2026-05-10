import time
from fastapi import APIRouter, HTTPException
from app.models.schemas import (
    InterviewStartRequest, InterviewStartResponse,
    InterviewEvaluateRequest, InterviewEvaluateResponse,
    AnalyzeErrorDetail,
)
from app.services.mock_service import get_area_questions
from app.services.openai_service import (
    get_openai_service, AIResponseError, AIServiceUnavailableError,
)
from app.services.event_store import record_event, AnalysisEvent

router = APIRouter(prefix="/interview", tags=["interview"])


def _pick_next_question(area: str, question_number: int, total: int) -> str | None:
    if question_number >= total:
        return None
    questions = get_area_questions(area)
    idx = question_number  # question_number is 1-based; next is index question_number
    return questions[idx] if idx < len(questions) else None


@router.post("/start", response_model=InterviewStartResponse)
async def start_interview(req: InterviewStartRequest):
    questions = get_area_questions(req.area)
    record_event(AnalysisEvent(
        event_type="interview_start",
        session_id=req.session_id,
    ))
    return InterviewStartResponse(
        session_id=req.session_id,
        first_question=questions[0],
        total_questions=min(5, len(questions)),
        area=req.area,
        level=req.level,
    )


@router.post("/evaluate", response_model=InterviewEvaluateResponse)
async def evaluate_answer(req: InterviewEvaluateRequest):
    wall_start = time.perf_counter()

    try:
        svc = get_openai_service()
        validated = await svc.evaluate_interview(
            session_id=req.session_id,
            area=req.area,
            level=req.level,
            question=req.question,
            answer=req.answer,
        )
    except AIResponseError as exc:
        raise HTTPException(
            status_code=422,
            detail=AnalyzeErrorDetail(
                error_code="AI_RESPONSE_INVALID",
                error_message=exc.user_message,
                session_id=req.session_id,
                repair_attempted=True,
            ).model_dump(),
        ) from exc
    except AIServiceUnavailableError as exc:
        raise HTTPException(
            status_code=503,
            detail=AnalyzeErrorDetail(
                error_code="AI_SERVICE_UNAVAILABLE",
                error_message=exc.user_message,
                session_id=req.session_id,
            ).model_dump(),
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=AnalyzeErrorDetail(
                error_code="INTERNAL_ERROR",
                error_message="Erro interno ao avaliar resposta. Tente novamente.",
                session_id=req.session_id,
            ).model_dump(),
        ) from exc

    elapsed_ms = int((time.perf_counter() - wall_start) * 1000)
    is_last = req.question_number >= req.total_questions
    next_q = _pick_next_question(req.area, req.question_number, req.total_questions)

    record_event(AnalysisEvent(
        event_type="interview_evaluate",
        session_id=req.session_id,
        latency_ms=elapsed_ms,
        score=validated.score,
    ))

    return InterviewEvaluateResponse(
        session_id=req.session_id,
        interview_id=req.interview_id,
        question_number=req.question_number,
        total_questions=req.total_questions,
        score=validated.score,
        accuracy=validated.accuracy,
        clarity=validated.clarity,
        depth=validated.depth,
        strengths=validated.strengths,
        weaknesses=validated.weaknesses,
        improved_answer=validated.improved_answer,
        next_question=next_q,
        completed=is_last,
    )
