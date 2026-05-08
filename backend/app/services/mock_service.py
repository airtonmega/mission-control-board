"""Mock AI service — used when USE_MOCK_AI=true (no API key required)."""
import time
import uuid
from app.models.schemas import (
    AnalyzeTextRequest, AnalyzeTextResponse,
    AnalyzeImageRequest, AnalyzeImageResponse,
    InterviewStartRequest, InterviewStartResponse,
    InterviewEvaluateRequest, InterviewEvaluateResponse,
)

_MOCK_THEMES = {
    "kotlin": "Kotlin — Linguagem e Sintaxe",
    "coroutine": "Kotlin — Coroutines & Concorrência",
    "compose": "Android — Jetpack Compose",
    "hilt": "Android — Injeção de Dependência (Hilt)",
    "room": "Android — Persistência (Room)",
    "viewmodel": "Android — ViewModel & StateFlow",
    "retrofit": "Android — Rede (Retrofit)",
    "solid": "Arquitetura — Princípios SOLID",
    "clean": "Arquitetura — Clean Architecture",
    "mvvm": "Arquitetura — MVVM",
    "flow": "Kotlin — Flow & Reatividade",
    "default": "Engenharia de Software — Conceitos Gerais",
}

_MOCK_QUESTIONS = [
    "Explique como funcionam as Coroutines no Kotlin.",
    "Qual a diferença entre StateFlow e SharedFlow?",
    "Como o Hilt facilita a injeção de dependência no Android?",
    "Descreva o padrão MVVM e suas vantagens.",
    "Como funciona o ciclo de vida de um Composable?",
]


def _detect_theme(question: str) -> str:
    q = question.lower()
    for keyword, theme in _MOCK_THEMES.items():
        if keyword in q:
            return theme
    return _MOCK_THEMES["default"]


async def mock_analyze_text(req: AnalyzeTextRequest) -> AnalyzeTextResponse:
    start = time.time()
    theme = _detect_theme(req.question)

    response = AnalyzeTextResponse(
        session_id=req.session_id,
        detected_theme=theme,
        quick_tip=(
            "Use structured concurrency com `supervisorScope` para isolar falhas "
            "sem cancelar toda a hierarquia de coroutines."
        ),
        short_answer=(
            f"Em resposta à pergunta sobre **{theme}**: este é um tópico central "
            "no desenvolvimento Android moderno. A resposta resumida envolve entender "
            "os fundamentos e aplicar boas práticas de arquitetura."
        ),
        interview_answer=(
            f"Em uma entrevista, eu abordaria **{theme}** destacando três pontos:\n\n"
            "1. **Fundamento teórico** — o que é e por que existe;\n"
            "2. **Aplicação prática** — como uso no dia a dia;\n"
            "3. **Trade-offs** — quando usar e quando evitar.\n\n"
            "Isso demonstra profundidade técnica e pensamento crítico ao entrevistador."
        ),
        complete_answer=(
            f"## {theme}\n\n"
            "### Conceito\n"
            "Este é um conceito fundamental na stack Android/Kotlin moderna. "
            "Compreender este tópico em profundidade é essencial para desenvolvedores "
            "de nível pleno e sênior.\n\n"
            "### Por que importa\n"
            "- Aumenta a legibilidade e manutenibilidade do código\n"
            "- Reduz bugs em cenários concorrentes\n"
            "- É amplamente cobrado em entrevistas técnicas\n\n"
            "### Exemplo prático\n"
            "```kotlin\n"
            "// Exemplo de código gerado pelo mock\n"
            "viewModelScope.launch {\n"
            "    val result = repository.fetchData()\n"
            "    _uiState.update { it.copy(data = result) }\n"
            "}\n"
            "```\n\n"
            "### Referências\n"
            "- Documentação oficial do Android Developers\n"
            "- Kotlin documentation\n"
            "- Android Architecture Guides"
        ),
        common_errors=[
            "Lançar coroutines fora de um CoroutineScope controlado (memory leak)",
            "Usar `GlobalScope` em produção sem justificativa",
            "Não tratar exceções com `CoroutineExceptionHandler`",
            "Confundir `launch` com `async/await` nos casos de uso",
        ],
        study_suggestions=[
            "Kotlin Coroutines — documentação oficial kotlinlang.org",
            "Curso 'Android Kotlin Fundamentals' no developer.android.com",
            "Livro: 'Kotlin Coroutines Deep Dive' — Marcin Moskała",
            "Praticar implementando um app com múltiplas fontes de dados assíncronas",
        ],
        confidence_score=0.82,
        processing_time_ms=int((time.time() - start) * 1000) + 120,
        mock=True,
    )
    return response


async def mock_analyze_image(req: AnalyzeImageRequest) -> AnalyzeImageResponse:
    return AnalyzeImageResponse(
        session_id=req.session_id,
        detected_text="[MOCK] Texto detectado na imagem: código Kotlin com padrão Repository",
        analysis="[MOCK] A imagem contém um diagrama de arquitetura MVVM com camadas bem definidas.",
        processing_time_ms=85,
        mock=True,
    )


async def mock_interview_start(req: InterviewStartRequest) -> InterviewStartResponse:
    return InterviewStartResponse(
        session_id=req.session_id,
        first_question=_MOCK_QUESTIONS[0],
        total_questions=5,
        area=req.area,
        level=req.level,
    )


async def mock_interview_evaluate(req: InterviewEvaluateRequest) -> InterviewEvaluateResponse:
    return InterviewEvaluateResponse(
        session_id=req.session_id,
        interview_id=req.interview_id,
        score=7.5,
        feedback=(
            "Boa resposta! Você cobriu os pontos principais. "
            "Para nota máxima, adicione exemplos práticos e mencione trade-offs."
        ),
        next_question=_MOCK_QUESTIONS[1],
        completed=False,
    )
