from fastapi import APIRouter, Depends
from app.core.config import Settings, get_settings
from app.models.schemas import (
    InterviewStartRequest, InterviewStartResponse,
    InterviewEvaluateRequest, InterviewEvaluateResponse,
)
from app.services import mock_service

router = APIRouter(prefix="/interview", tags=["interview"])


@router.post("/start", response_model=InterviewStartResponse)
async def start_interview(
    req: InterviewStartRequest,
    settings: Settings = Depends(get_settings),
):
    return await mock_service.mock_interview_start(req)


@router.post("/evaluate", response_model=InterviewEvaluateResponse)
async def evaluate_answer(
    req: InterviewEvaluateRequest,
    settings: Settings = Depends(get_settings),
):
    return await mock_service.mock_interview_evaluate(req)
