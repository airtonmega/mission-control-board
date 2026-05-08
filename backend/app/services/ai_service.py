"""Real AI service — delegates to OpenAI when USE_MOCK_AI=false."""
import time
import json
from openai import AsyncOpenAI
from app.core.config import get_settings
from app.models.schemas import AnalyzeTextRequest, AnalyzeTextResponse

_SYSTEM_PROMPT = """Você é um assistente acadêmico especializado em entrevistas técnicas de Engenharia de Software, Android e Kotlin.

Ao receber uma pergunta técnica, responda EXCLUSIVAMENTE com um JSON válido no seguinte schema:
{
  "detected_theme": "string — tema principal identificado",
  "quick_tip": "string — dica prática em 1-2 frases",
  "short_answer": "string — resposta objetiva em 2-3 frases",
  "interview_answer": "string — resposta ideal para uma entrevista técnica (markdown, 3-5 parágrafos)",
  "complete_answer": "string — explicação completa com exemplos de código (markdown)",
  "common_errors": ["array de strings — erros frequentes relacionados ao tema"],
  "study_suggestions": ["array de strings — recursos e sugestões de estudo"],
  "confidence_score": 0.0
}

Responda sempre no idioma da pergunta. Nunca adicione texto fora do JSON."""


async def real_analyze_text(req: AnalyzeTextRequest) -> AnalyzeTextResponse:
    settings = get_settings()
    client = AsyncOpenAI(api_key=settings.openai_api_key)
    start = time.time()

    messages = [
        {"role": "system", "content": _SYSTEM_PROMPT},
        {"role": "user", "content": req.question},
    ]
    if req.context:
        messages.insert(1, {"role": "user", "content": f"Contexto adicional: {req.context}"})

    completion = await client.chat.completions.create(
        model=settings.openai_model,
        messages=messages,
        response_format={"type": "json_object"},
        temperature=0.7,
        max_tokens=2000,
    )

    raw = json.loads(completion.choices[0].message.content)
    elapsed = int((time.time() - start) * 1000)

    return AnalyzeTextResponse(
        session_id=req.session_id,
        detected_theme=raw.get("detected_theme", "Geral"),
        quick_tip=raw.get("quick_tip", ""),
        short_answer=raw.get("short_answer", ""),
        interview_answer=raw.get("interview_answer", ""),
        complete_answer=raw.get("complete_answer", ""),
        common_errors=raw.get("common_errors", []),
        study_suggestions=raw.get("study_suggestions", []),
        confidence_score=float(raw.get("confidence_score", 0.8)),
        processing_time_ms=elapsed,
        mock=False,
    )
