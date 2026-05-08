from fastapi import APIRouter, Depends, HTTPException
from app.core.config import Settings, get_settings
from app.models.schemas import (
    AnalyzeTextRequest, AnalyzeTextResponse,
    AnalyzeImageRequest, AnalyzeImageResponse,
)
from app.services import mock_service, ai_service

router = APIRouter(prefix="/analyze", tags=["analyze"])


@router.post("/text", response_model=AnalyzeTextResponse)
async def analyze_text(
    req: AnalyzeTextRequest,
    settings: Settings = Depends(get_settings),
):
    if settings.use_mock_ai or not settings.openai_api_key:
        return await mock_service.mock_analyze_text(req)
    try:
        return await ai_service.real_analyze_text(req)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"AI service error: {exc}") from exc


@router.post("/image", response_model=AnalyzeImageResponse)
async def analyze_image(
    req: AnalyzeImageRequest,
    settings: Settings = Depends(get_settings),
):
    return await mock_service.mock_analyze_image(req)
