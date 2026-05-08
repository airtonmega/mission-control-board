import uuid
import hashlib
import time
from fastapi import APIRouter, Depends
from app.core.config import Settings, get_settings
from app.models.schemas import AnonymousAuthResponse

router = APIRouter(prefix="/auth", tags=["auth"])


def _create_token(session_id: str, settings: Settings) -> str:
    payload = f"{session_id}:{int(time.time())}:{settings.jwt_secret}"
    digest = hashlib.sha256(payload.encode()).hexdigest()
    return f"tese_{session_id[:8]}_{digest[:32]}"


@router.post("/anonymous", response_model=AnonymousAuthResponse)
async def create_anonymous_session(settings: Settings = Depends(get_settings)):
    session_id = str(uuid.uuid4())
    token = _create_token(session_id, settings)
    return AnonymousAuthResponse(session_id=session_id, token=token)
