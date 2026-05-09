import logging

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile

from app.core.config import Settings, get_settings
from app.models.schemas import (
    AnalyzeAudioResponse,
    AnalyzeErrorDetail,
    AnalyzeImageResponse,
    AnalyzeTextRequest,
    AnalyzeTextResponse,
)
from app.services import mock_service
from app.services.openai_service import (
    AIResponseError,
    AIServiceUnavailableError,
    OpenAIService,
    get_openai_service,
)
from app.services.event_store import record_event, AnalysisEvent

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/analyze", tags=["analyze"])


def _should_use_mock(settings: Settings) -> bool:
    return settings.use_mock_ai or not settings.openai_api_key.strip()


@router.post("/text", response_model=AnalyzeTextResponse)
async def analyze_text(
    req: AnalyzeTextRequest,
    settings: Settings = Depends(get_settings),
) -> AnalyzeTextResponse:
    if _should_use_mock(settings):
        logger.debug("analyze_text using mock session_id=%s", req.session_id)
        response = await mock_service.mock_analyze_text(req)
        record_event(AnalysisEvent(
            event_type="text",
            session_id=req.session_id,
            latency_ms=response.processing_time_ms,
            theme=response.detected_theme,
            mock=response.mock,
        ))
        return response

    svc: OpenAIService = get_openai_service()

    try:
        response = await svc.analyze_text(req)
        record_event(AnalysisEvent(
            event_type="text",
            session_id=req.session_id,
            latency_ms=response.processing_time_ms,
            theme=response.detected_theme,
            mock=response.mock,
        ))
        return response

    except AIResponseError as exc:
        logger.warning(
            "analyze_text AIResponseError session_id=%s detail=%s",
            req.session_id,
            exc.technical_detail,
        )
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
        logger.error(
            "analyze_text AIServiceUnavailableError session_id=%s detail=%s",
            req.session_id,
            exc.technical_detail,
        )
        raise HTTPException(
            status_code=503,
            detail=AnalyzeErrorDetail(
                error_code="AI_SERVICE_UNAVAILABLE",
                error_message=exc.user_message,
                session_id=req.session_id,
            ).model_dump(),
        ) from exc

    except Exception as exc:
        logger.exception("analyze_text unexpected error session_id=%s", req.session_id)
        raise HTTPException(
            status_code=500,
            detail=AnalyzeErrorDetail(
                error_code="INTERNAL_ERROR",
                error_message="Erro interno. Tente novamente em alguns instantes.",
                session_id=req.session_id,
            ).model_dump(),
        ) from exc


@router.post("/image", response_model=AnalyzeImageResponse)
async def analyze_image(
    session_id: str = Form(...),
    image: UploadFile = File(...),
    settings: Settings = Depends(get_settings),
) -> AnalyzeImageResponse:
    if _should_use_mock(settings):
        logger.debug("analyze_image using mock session_id=%s", session_id)
        response = await mock_service.mock_analyze_image(session_id)
        record_event(AnalysisEvent(
            event_type="image",
            session_id=session_id,
            latency_ms=response.processing_time_ms,
            theme=response.detected_theme,
            mock=response.mock,
        ))
        return response

    svc: OpenAIService = get_openai_service()
    image_bytes = await image.read()
    content_type = image.content_type or "image/jpeg"

    try:
        response = await svc.analyze_image(session_id, image_bytes, content_type)
        record_event(AnalysisEvent(
            event_type="image",
            session_id=session_id,
            latency_ms=response.processing_time_ms,
            theme=response.detected_theme,
            mock=response.mock,
        ))
        return response

    except AIResponseError as exc:
        logger.warning(
            "analyze_image AIResponseError session_id=%s detail=%s",
            session_id,
            exc.technical_detail,
        )
        raise HTTPException(
            status_code=422,
            detail=AnalyzeErrorDetail(
                error_code="AI_RESPONSE_INVALID",
                error_message=exc.user_message,
                session_id=session_id,
                repair_attempted=True,
            ).model_dump(),
        ) from exc

    except AIServiceUnavailableError as exc:
        logger.error(
            "analyze_image AIServiceUnavailableError session_id=%s detail=%s",
            session_id,
            exc.technical_detail,
        )
        raise HTTPException(
            status_code=503,
            detail=AnalyzeErrorDetail(
                error_code="AI_SERVICE_UNAVAILABLE",
                error_message=exc.user_message,
                session_id=session_id,
            ).model_dump(),
        ) from exc

    except Exception as exc:
        logger.exception("analyze_image unexpected error session_id=%s", session_id)
        raise HTTPException(
            status_code=500,
            detail=AnalyzeErrorDetail(
                error_code="INTERNAL_ERROR",
                error_message="Erro interno. Tente novamente em alguns instantes.",
                session_id=session_id,
            ).model_dump(),
        ) from exc


@router.post("/audio", response_model=AnalyzeAudioResponse)
async def analyze_audio(
    session_id: str = Form(...),
    audio: UploadFile = File(...),
    settings: Settings = Depends(get_settings),
) -> AnalyzeAudioResponse:
    if _should_use_mock(settings):
        logger.debug("analyze_audio using mock session_id=%s", session_id)
        response = await mock_service.mock_analyze_audio(session_id)
        record_event(AnalysisEvent(
            event_type="audio",
            session_id=session_id,
            latency_ms=response.processing_time_ms,
            theme=response.detected_theme,
            mock=response.mock,
        ))
        return response

    svc: OpenAIService = get_openai_service()
    audio_bytes = await audio.read()
    content_type = audio.content_type or "audio/m4a"

    try:
        response = await svc.analyze_audio(session_id, audio_bytes, content_type)
        record_event(AnalysisEvent(
            event_type="audio",
            session_id=session_id,
            latency_ms=response.processing_time_ms,
            theme=response.detected_theme,
            mock=response.mock,
        ))
        return response

    except AIResponseError as exc:
        logger.warning(
            "analyze_audio AIResponseError session_id=%s detail=%s",
            session_id,
            exc.technical_detail,
        )
        raise HTTPException(
            status_code=422,
            detail=AnalyzeErrorDetail(
                error_code="AI_RESPONSE_INVALID",
                error_message=exc.user_message,
                session_id=session_id,
                repair_attempted=True,
            ).model_dump(),
        ) from exc

    except AIServiceUnavailableError as exc:
        logger.error(
            "analyze_audio AIServiceUnavailableError session_id=%s detail=%s",
            session_id,
            exc.technical_detail,
        )
        raise HTTPException(
            status_code=503,
            detail=AnalyzeErrorDetail(
                error_code="AI_SERVICE_UNAVAILABLE",
                error_message=exc.user_message,
                session_id=session_id,
            ).model_dump(),
        ) from exc

    except Exception as exc:
        logger.exception("analyze_audio unexpected error session_id=%s", session_id)
        raise HTTPException(
            status_code=500,
            detail=AnalyzeErrorDetail(
                error_code="INTERNAL_ERROR",
                error_message="Erro interno. Tente novamente em alguns instantes.",
                session_id=session_id,
            ).model_dump(),
        ) from exc
