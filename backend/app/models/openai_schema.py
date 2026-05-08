"""Internal Pydantic model for validating raw OpenAI JSON output.

Kept separate from the public API schemas so that validation rules can
be tightened here without affecting the external contract.
"""
from pydantic import BaseModel, Field, field_validator


class OpenAIRawResponse(BaseModel):
    """Validates and coerces the JSON object returned by OpenAI."""

    detected_theme: str = Field(min_length=1, max_length=200)
    quick_tip: str = Field(min_length=1, max_length=500)
    short_answer: str = Field(min_length=1, max_length=1000)
    interview_answer: str = Field(min_length=1, max_length=4000)
    complete_answer: str = Field(min_length=1, max_length=8000)
    common_errors: list[str] = Field(min_length=2, max_length=8)
    study_suggestions: list[str] = Field(min_length=2, max_length=6)
    confidence_score: float = Field(ge=0.0, le=1.0)

    @field_validator("common_errors", "study_suggestions", mode="before")
    @classmethod
    def ensure_non_empty_strings(cls, v: list) -> list:
        return [str(item).strip() for item in v if str(item).strip()]

    @field_validator("confidence_score", mode="before")
    @classmethod
    def coerce_confidence(cls, v) -> float:
        try:
            return float(v)
        except (TypeError, ValueError):
            return 0.7


# JSON Schema constant used in the repair prompt to remind the model of
# the exact structure required. Not used for OpenAI structured-output mode
# (we rely on json_object + Pydantic validation), but embedded in prompts.
RESPONSE_JSON_SCHEMA = {
    "type": "object",
    "required": [
        "detected_theme",
        "quick_tip",
        "short_answer",
        "interview_answer",
        "complete_answer",
        "common_errors",
        "study_suggestions",
        "confidence_score",
    ],
    "properties": {
        "detected_theme": {"type": "string", "minLength": 1},
        "quick_tip": {"type": "string", "minLength": 1},
        "short_answer": {"type": "string", "minLength": 1},
        "interview_answer": {"type": "string", "minLength": 1},
        "complete_answer": {"type": "string", "minLength": 1},
        "common_errors": {"type": "array", "items": {"type": "string"}, "minItems": 2},
        "study_suggestions": {"type": "array", "items": {"type": "string"}, "minItems": 2},
        "confidence_score": {"type": "number", "minimum": 0.0, "maximum": 1.0},
    },
    "additionalProperties": False,
}
