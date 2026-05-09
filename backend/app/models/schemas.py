from pydantic import BaseModel, Field
from typing import Optional
import uuid


# ── Auth ──────────────────────────────────────────────────────────────────────

class AnonymousAuthResponse(BaseModel):
    session_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    token: str
    message: str = "Session created"


# ── Consent ───────────────────────────────────────────────────────────────────

class ConsentAcceptRequest(BaseModel):
    session_id: str
    accepted: bool
    timestamp: str


class ConsentAcceptResponse(BaseModel):
    session_id: str
    consent_recorded: bool
    message: str


# ── Analyze ───────────────────────────────────────────────────────────────────

class AnalyzeTextRequest(BaseModel):
    session_id: str
    question: str = Field(..., min_length=3, max_length=2000)
    context: Optional[str] = None
    language: str = "pt-BR"


class AnalyzeTextResponse(BaseModel):
    session_id: str
    detected_theme: str
    quick_tip: str
    short_answer: str
    interview_answer: str
    complete_answer: str
    common_errors: list[str]
    study_suggestions: list[str]
    confidence_score: float = Field(ge=0.0, le=1.0)
    processing_time_ms: int
    mock: bool = False


class AnalyzeImageResponse(BaseModel):
    """Response for /analyze/image.

    Mirrors AnalyzeTextResponse field-for-field so Android can reuse the
    same card composables, plus `detected_text` for OCR output.
    """
    session_id: str
    detected_text: str
    detected_theme: str
    quick_tip: str
    short_answer: str
    interview_answer: str
    complete_answer: str
    common_errors: list[str]
    study_suggestions: list[str]
    confidence_score: float = Field(ge=0.0, le=1.0)
    processing_time_ms: int
    mock: bool = True


class AnalyzeAudioResponse(BaseModel):
    """Response for /analyze/audio.

    Mirrors AnalyzeTextResponse plus `transcription` with the Whisper output.
    """
    session_id: str
    transcription: str
    detected_theme: str
    quick_tip: str
    short_answer: str
    interview_answer: str
    complete_answer: str
    common_errors: list[str]
    study_suggestions: list[str]
    confidence_score: float = Field(ge=0.0, le=1.0)
    processing_time_ms: int
    mock: bool = False


# ── Interview ─────────────────────────────────────────────────────────────────

class InterviewStartRequest(BaseModel):
    session_id: str
    area: str = "Engenharia de Software"
    level: str = "Pleno"
    duration_minutes: int = 30


class InterviewStartResponse(BaseModel):
    session_id: str
    interview_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    first_question: str
    total_questions: int
    area: str
    level: str


class InterviewEvaluateRequest(BaseModel):
    session_id: str
    interview_id: str
    question: str
    answer: str
    question_number: int = 1
    total_questions: int = 5
    area: str = "Engenharia de Software"
    level: str = "Pleno"


class InterviewEvaluateResponse(BaseModel):
    session_id: str
    interview_id: str
    question_number: int
    total_questions: int
    score: float = Field(ge=0.0, le=10.0)
    accuracy: float = Field(ge=0.0, le=10.0)
    clarity: float = Field(ge=0.0, le=10.0)
    depth: float = Field(ge=0.0, le=10.0)
    strengths: list[str]
    weaknesses: list[str]
    improved_answer: str
    next_question: Optional[str] = None
    completed: bool = False
    mock: bool = False


# ── Reports ───────────────────────────────────────────────────────────────────

class InputModeCount(BaseModel):
    text: int = 0
    image: int = 0
    audio: int = 0
    interview: int = 0


class SessionReport(BaseModel):
    session_id: str
    total_queries: int
    topics_covered: list[str]
    average_confidence: float
    duration_seconds: int
    created_at: str
    highlights: list[str]
    input_modes: InputModeCount = Field(default_factory=InputModeCount)
    average_latency_ms: Optional[float] = None
    interview_average_score: Optional[float] = None
    events: list[dict] = Field(default_factory=list)


# ── Errors ────────────────────────────────────────────────────────────────────

class AnalyzeErrorDetail(BaseModel):
    """Structured error body returned inside FastAPI HTTPException.detail.

    Android parses this via the outer {"detail": {...}} envelope.
    """
    error_code: str
    error_message: str
    session_id: str
    repair_attempted: bool = False


# ── Health ────────────────────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str = "ok"
    version: str
    mock_mode: bool
    message: str
