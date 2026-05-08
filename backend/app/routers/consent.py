from fastapi import APIRouter
from app.models.schemas import ConsentAcceptRequest, ConsentAcceptResponse

router = APIRouter(prefix="/consent", tags=["consent"])

_consent_store: dict[str, bool] = {}


@router.post("/accept", response_model=ConsentAcceptResponse)
async def accept_consent(req: ConsentAcceptRequest):
    _consent_store[req.session_id] = req.accepted
    return ConsentAcceptResponse(
        session_id=req.session_id,
        consent_recorded=True,
        message="Consentimento registrado com sucesso." if req.accepted else "Consentimento negado.",
    )
