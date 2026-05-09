"""Mock AI service — used when USE_MOCK_AI=true (no API key required)."""
import time
from typing import Optional
from app.models.schemas import (
    AnalyzeAudioResponse,
    AnalyzeTextRequest, AnalyzeTextResponse,
    AnalyzeImageResponse,
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

_AREA_QUESTIONS: dict[str, list[str]] = {
    "Android": [
        "O que são Kotlin Coroutines? Como diferem das threads Java e quando você usaria cada abordagem?",
        "Explique o padrão MVVM no Android moderno. Quais são as responsabilidades de cada camada?",
        "Qual a diferença entre StateFlow e SharedFlow? Dê exemplos de quando usar cada um.",
        "Como o Hilt implementa injeção de dependência? O que são @Singleton e @HiltViewModel e como escolher?",
        "Explique o ciclo de vida no Jetpack Compose. O que é recomposição e como evitar recomposições desnecessárias?",
    ],
    "Backend": [
        "Qual a diferença entre REST e GraphQL? Quando você escolheria um sobre o outro?",
        "O que é autenticação JWT? Explique o fluxo e quais são os principais riscos de segurança.",
        "Explique o CAP Theorem com um exemplo prático. Como ele afeta a escolha do banco de dados?",
        "O que são microsserviços? Quais os trade-offs em relação a uma arquitetura monolítica?",
        "Como você implementaria cache em uma API de alto tráfego? Quais estratégias de invalidação usaria?",
    ],
    "Full Stack": [
        "Explique o event loop do JavaScript. Como funcionam Promises e async/await sob o capô?",
        "O que é SSR (Server Side Rendering)? Compare com CSR e SSG — quando usar cada um?",
        "Como você otimizaria a performance de uma aplicação web com alto tráfego?",
        "Explique CORS. Por que existe e como configurar corretamente em uma API?",
        "Qual a diferença entre autenticação baseada em sessão e JWT? Quais os trade-offs?",
    ],
    "Data Science": [
        "Explique o bias-variance tradeoff. Como ele afeta a escolha e o tuning de modelos?",
        "O que é overfitting? Quais técnicas de regularização você conhece e quando aplicar cada uma?",
        "Explique precisão, recall e F1-score. Em qual situação cada métrica é mais relevante?",
        "O que são transformers e por que revolucionaram o processamento de linguagem natural?",
        "Como você lidaria com dados desbalanceados em um problema de classificação binária?",
    ],
    "DevOps": [
        "Explique Infrastructure as Code. Quais ferramentas você usa e por que as escolheu?",
        "Descreva um pipeline CI/CD completo e robusto. Quais etapas são indispensáveis?",
        "Como funciona Kubernetes? Explique a relação entre pods, deployments, services e ingress.",
        "O que é observabilidade? Como você a implementaria em um sistema distribuído com microserviços?",
        "Compare blue/green deployment com canary release. Quando usar cada estratégia?",
    ],
    "Arquitetura": [
        "Explique os princípios SOLID com exemplos práticos de violações comuns no dia a dia.",
        "O que é Domain-Driven Design? Como você identifica e delimita bounded contexts?",
        "Como você decide entre arquitetura monolítica e microsserviços para um novo sistema?",
        "Explique o padrão CQRS. Em quais cenários ele traz benefícios reais e quando evitá-lo?",
        "O que são Design Patterns? Explique Factory Method, Observer e Strategy com exemplos práticos.",
    ],
}

_DEFAULT_AREA = "Android"

_MOCK_SCORE_PROFILES = [
    (7.5, 7.2, 8.0, 7.3),
    (8.2, 8.0, 8.5, 8.0),
    (6.8, 6.5, 7.2, 6.8),
    (7.9, 7.6, 8.1, 8.0),
    (7.3, 7.0, 7.5, 7.2),
]

_MOCK_STRENGTHS_POOL = [
    "Demonstrou conhecimento sólido do conceito central",
    "Usou terminologia técnica correta",
    "Abordou o tema de forma estruturada",
    "Identificou casos de uso práticos relevantes",
    "Mencionou trade-offs importantes",
]

_MOCK_WEAKNESSES_POOL = [
    "Poderia incluir exemplos de código concretos",
    "Faltou mencionar casos de uso avançados ou edge cases",
    "A explicação poderia ser mais objetiva e direta",
    "Não mencionou as principais alternativas e suas diferenças",
]


def get_area_questions(area: str) -> list[str]:
    return _AREA_QUESTIONS.get(area, _AREA_QUESTIONS[_DEFAULT_AREA])


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
            "Este é um conceito fundamental na stack Android/Kotlin moderna.\n\n"
            "### Por que importa\n"
            "- Aumenta a legibilidade e manutenibilidade do código\n"
            "- Reduz bugs em cenários concorrentes\n"
            "- É amplamente cobrado em entrevistas técnicas\n\n"
            "### Exemplo prático\n"
            "```kotlin\n"
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


async def mock_analyze_image(session_id: str) -> AnalyzeImageResponse:
    start = time.time()
    theme = "Android — Arquitetura MVVM"
    return AnalyzeImageResponse(
        session_id=session_id,
        detected_text="[MOCK] Texto detectado: código Kotlin com padrão Repository e ViewModel",
        detected_theme=theme,
        quick_tip="Separe responsabilidades: ViewModel não deve conhecer View diretamente.",
        short_answer=(
            f"A imagem mostra uma implementação de **{theme}**. "
            "O padrão separa UI (View), lógica de apresentação (ViewModel) e dados (Repository)."
        ),
        interview_answer=(
            f"Em uma entrevista, eu descreveria **{theme}** em três camadas:\n\n"
            "1. **View** — Composables que observam StateFlow;\n"
            "2. **ViewModel** — Expõe estado imutável, não conhece a View;\n"
            "3. **Repository** — Única fonte de verdade."
        ),
        complete_answer=(
            f"## {theme}\n\n"
            "MVVM é o padrão arquitetural recomendado pelo Google.\n\n"
            "```kotlin\n"
            "viewModelScope.launch {\n"
            "    repo.analyzeText(question)\n"
            "        .onSuccess { _state.value = UiState.Success(it) }\n"
            "}\n"
            "```"
        ),
        common_errors=[
            "Passar Context para o ViewModel (memory leak)",
            "Lógica de negócio diretamente no Composable",
            "Múltiplos StateFlows em vez de um UiState selado",
        ],
        study_suggestions=[
            "Guide to app architecture — developer.android.com",
            "Now in Android sample app (GitHub/android/nowinandroid)",
        ],
        confidence_score=0.87,
        processing_time_ms=int((time.time() - start) * 1000) + 85,
        mock=True,
    )


async def mock_analyze_audio(session_id: str) -> AnalyzeAudioResponse:
    start = time.time()
    transcription = (
        "[MOCK] O que são Kotlin Coroutines e como elas diferem "
        "das threads tradicionais do Java?"
    )
    theme = _detect_theme(transcription)
    return AnalyzeAudioResponse(
        session_id=session_id,
        transcription=transcription,
        detected_theme=theme,
        quick_tip="Coroutines são leves: crie milhares sem overhead de threads do SO.",
        short_answer=(
            f"**{theme}** são unidades de computação suspensáveis que rodam em threads "
            "de forma cooperativa, sem bloqueio."
        ),
        interview_answer=(
            f"Em uma entrevista eu abordaria **{theme}** em três camadas:\n\n"
            "1. **Custo**: coroutine custa ~1 KB de heap vs ~1 MB de stack;\n"
            "2. **Suspensão vs bloqueio**: `suspend` libera a thread;\n"
            "3. **Structured concurrency**: `viewModelScope` cancela ao sair do VM."
        ),
        complete_answer=(
            f"## {theme}\n\n"
            "Coroutines permitem código assíncrono com sintaxe sequencial.\n\n"
            "```kotlin\n"
            "viewModelScope.launch {\n"
            "    val data = withContext(Dispatchers.IO) { api.fetch() }\n"
            "    _state.value = UiState.Success(data)\n"
            "}\n"
            "```"
        ),
        common_errors=[
            "Usar `GlobalScope` — sem ciclo de vida controlado",
            "Bloquear a main thread com `runBlocking` em produção",
            "Não tratar `CancellationException` separadamente",
        ],
        study_suggestions=[
            "Kotlin Coroutines Guide — kotlinlang.org/docs/coroutines-guide.html",
            "Livro: 'Kotlin Coroutines Deep Dive' — Marcin Moskała",
        ],
        confidence_score=0.88,
        processing_time_ms=int((time.time() - start) * 1000) + 180,
        mock=True,
    )


async def mock_interview_start(req: InterviewStartRequest) -> InterviewStartResponse:
    questions = get_area_questions(req.area)
    return InterviewStartResponse(
        session_id=req.session_id,
        first_question=questions[0],
        total_questions=min(5, len(questions)),
        area=req.area,
        level=req.level,
    )


async def mock_interview_evaluate(req: InterviewEvaluateRequest) -> InterviewEvaluateResponse:
    questions = get_area_questions(req.area)
    total = req.total_questions
    q_idx = req.question_number - 1
    profile_idx = q_idx % len(_MOCK_SCORE_PROFILES)
    score, accuracy, clarity, depth = _MOCK_SCORE_PROFILES[profile_idx]

    # Adjust slightly based on answer length (longer = marginally higher)
    answer_len = len(req.answer.strip())
    bonus = min(1.0, answer_len / 500)
    score = min(10.0, round(score + bonus * 0.5, 1))
    accuracy = min(10.0, round(accuracy + bonus * 0.4, 1))
    clarity = min(10.0, round(clarity + bonus * 0.3, 1))
    depth = min(10.0, round(depth + bonus * 0.6, 1))

    is_last = req.question_number >= total
    next_q: Optional[str] = None
    if not is_last:
        next_idx = req.question_number  # 1-based, so next is index question_number
        next_q = questions[next_idx] if next_idx < len(questions) else None

    strengths = _MOCK_STRENGTHS_POOL[q_idx % 2: q_idx % 2 + 2]
    weaknesses = _MOCK_WEAKNESSES_POOL[q_idx % 2: q_idx % 2 + 2]

    return InterviewEvaluateResponse(
        session_id=req.session_id,
        interview_id=req.interview_id,
        question_number=req.question_number,
        total_questions=total,
        score=score,
        accuracy=accuracy,
        clarity=clarity,
        depth=depth,
        strengths=strengths,
        weaknesses=weaknesses,
        improved_answer=(
            f"[MOCK — Pergunta {req.question_number}] Resposta ideal para '{req.question[:60]}...': "
            f"Uma resposta completa para nível {req.level} incluiria: "
            f"(1) definição precisa do conceito, (2) exemplo de código comentado, "
            f"(3) trade-offs e casos de uso reais, (4) referências à documentação oficial. "
            f"Área: {req.area}."
        ),
        next_question=next_q,
        completed=is_last,
        mock=True,
    )
