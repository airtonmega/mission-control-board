"""Endpoint para criação de sessão realtime segura.

O backend usa a OPENAI_API_KEY para obter um token efêmero da OpenAI.
O token é repassado ao Android, que conecta diretamente ao WebSocket da OpenAI.
A chave de API nunca é exposta ao app.
"""
import time
import logging
import httpx

from fastapi import APIRouter, Depends, HTTPException
from app.core.config import Settings, get_settings
from app.models.schemas import (
    RealtimeSessionRequest,
    RealtimeSessionResponse,
    RealtimeClientSecret,
    AnalyzeErrorDetail,
)
from app.services.event_store import record_event, AnalysisEvent

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/realtime", tags=["realtime"])

_OPENAI_REALTIME_SESSIONS_URL = "https://api.openai.com/v1/realtime/sessions"
_REALTIME_MODEL = "gpt-4o-realtime-preview-2024-12-17"


@router.post("/session", response_model=RealtimeSessionResponse)
async def create_realtime_session(
    req: RealtimeSessionRequest,
    settings: Settings = Depends(get_settings),
):
    """Cria uma sessão realtime e retorna token efêmero para o cliente Android."""
    wall_start = time.perf_counter()

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.post(
                _OPENAI_REALTIME_SESSIONS_URL,
                headers={
                    "Authorization": f"Bearer {settings.openai_api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": _REALTIME_MODEL,
                    "voice": req.voice,
                    "instructions": (
                        "Você é um assistente técnico acadêmico especializado em entrevistas "
                        "de tecnologia. Avalie, explique e treine o usuário em conceitos de "
                        "programação, arquitetura e engenharia de software. Responda em português "
                        "do Brasil de forma clara e didática."
                    ),
                    "input_audio_transcription": {"model": "whisper-1"},
                    "turn_detection": {
                        "type": "server_vad",
                        "threshold": 0.5,
                        "silence_duration_ms": 800,
                        "prefix_padding_ms": 300,
                    },
                },
            )
            response.raise_for_status()
            data = response.json()
    except httpx.TimeoutException as exc:
        raise HTTPException(
            status_code=503,
            detail=AnalyzeErrorDetail(
                error_code="AI_SERVICE_UNAVAILABLE",
                error_message="O serviço realtime demorou demais para responder. Tente novamente.",
                session_id=req.session_id,
            ).model_dump(),
        ) from exc
    except httpx.HTTPStatusError as exc:
        raise HTTPException(
            status_code=503,
            detail=AnalyzeErrorDetail(
                error_code="AI_SERVICE_UNAVAILABLE",
                error_message=f"Serviço realtime indisponível ({exc.response.status_code}). Tente novamente.",
                session_id=req.session_id,
            ).model_dump(),
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=AnalyzeErrorDetail(
                error_code="INTERNAL_ERROR",
                error_message="Erro interno ao criar sessão realtime.",
                session_id=req.session_id,
            ).model_dump(),
        ) from exc

    elapsed_ms = int((time.perf_counter() - wall_start) * 1000)
    record_event(AnalysisEvent(
        event_type="realtime_session",
        session_id=req.session_id,
        latency_ms=elapsed_ms,
    ))
    logger.info(
        "realtime_session_created session_id=%s latency_ms=%d model=%s",
        req.session_id, elapsed_ms, _REALTIME_MODEL,
    )

    client_secret = data.get("client_secret", {})
    return RealtimeSessionResponse(
        session_id=req.session_id,
        realtime_session_id=data.get("id", ""),
        client_secret=RealtimeClientSecret(
            value=client_secret.get("value", ""),
            expires_at=client_secret.get("expires_at", int(time.time()) + 60),
        ),
        model=data.get("model", _REALTIME_MODEL),
        expires_at=client_secret.get("expires_at", int(time.time()) + 60),
        voice=data.get("voice", req.voice),
    )
