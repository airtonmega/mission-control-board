from fastapi import APIRouter, Depends
from app.core.config import Settings, get_settings
from app.models.schemas import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health_check(settings: Settings = Depends(get_settings)):
    return HealthResponse(
        status="ok",
        version=settings.app_version,
        message="TeseAI Live API operacional",
    )
