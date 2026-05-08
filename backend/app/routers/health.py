from fastapi import APIRouter, Depends
from app.core.config import Settings, get_settings
from app.models.schemas import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health(settings: Settings = Depends(get_settings)):
    return HealthResponse(
        status="ok",
        version=settings.app_version,
        mock_mode=settings.use_mock_ai,
        message="TeseAI Live API operacional" + (" [MOCK MODE]" if settings.use_mock_ai else ""),
    )
