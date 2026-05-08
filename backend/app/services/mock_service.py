"""Mock AI service — used when USE_MOCK_AI=true (no API key required)."""
import time
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
            "1. **View** — Composables que observam StateFlow e recompõem conforme o estado;\n"
            "2. **ViewModel** — Expõe estado imutável, processa eventos, não conhece a View;\n"
            "3. **Repository** — Única fonte de verdade, abstrai Room e Retrofit.\n\n"
            "Isso promove testabilidade e separação clara de responsabilidades."
        ),
        complete_answer=(
            f"## {theme}\n\n"
            "### Visão Geral\n"
            "MVVM (Model-View-ViewModel) é o padrão arquitetural recomendado pelo Google para apps Android modernos.\n\n"
            "### Camadas\n"
            "- **View (Composables):** Observa `StateFlow<UiState>` via `collectAsStateWithLifecycle()`;\n"
            "- **ViewModel:** Gerencia estado com `MutableStateFlow`, chama Repository em coroutines;\n"
            "- **Repository:** Coordena fontes de dados (Room local + Retrofit remoto).\n\n"
            "### Exemplo\n"
            "```kotlin\n"
            "@HiltViewModel\n"
            "class CockpitViewModel @Inject constructor(\n"
            "    private val repo: AnalyzeRepository\n"
            ") : ViewModel() {\n"
            "    private val _state = MutableStateFlow<UiState>(UiState.Idle)\n"
            "    val state = _state.asStateFlow()\n\n"
            "    fun analyze(question: String) {\n"
            "        viewModelScope.launch {\n"
            "            _state.value = UiState.Loading\n"
            "            repo.analyzeText(question)\n"
            "                .onSuccess { _state.value = UiState.Success(it) }\n"
            "                .onFailure { _state.value = UiState.Error(it.message.orEmpty()) }\n"
            "        }\n"
            "    }\n"
            "}\n"
            "```\n\n"
            "### Benefícios\n"
            "- Testabilidade: ViewModel testável sem Android framework;\n"
            "- Reatividade: UI sempre sincronizada com o estado;\n"
            "- Sobrevive a mudanças de configuração (rotação)."
        ),
        common_errors=[
            "Passar Context para o ViewModel (memory leak)",
            "Lógica de negócio diretamente no Composable",
            "Múltiplos StateFlows em vez de um UiState selado",
            "Repository fazendo parse de UI — viola separação de camadas",
        ],
        study_suggestions=[
            "Guide to app architecture — developer.android.com",
            "Kotlin Flows — kotlinlang.org/docs/flow.html",
            "Now in Android sample app (GitHub/android/nowinandroid)",
            "Livro: 'Android Programming: The Big Nerd Ranch Guide'",
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
            "de forma cooperativa, sem bloqueio, ao contrário das threads Java que são gerenciadas "
            "pelo sistema operacional com custo de contexto alto."
        ),
        interview_answer=(
            f"Em uma entrevista eu abordaria **{theme}** em três camadas:\n\n"
            "1. **Custo**: coroutine custa ~1 KB de heap vs ~1 MB de stack de thread;\n"
            "2. **Suspensão vs bloqueio**: `suspend` libera a thread para outras tasks;\n"
            "3. **Structured concurrency**: `viewModelScope` cancela automaticamente ao "
            "sair do ViewModel, evitando leaks."
        ),
        complete_answer=(
            f"## {theme}\n\n"
            "### O que são\n"
            "Coroutines são uma abstração sobre threads que permitem código assíncrono "
            "com sintaxe sequencial usando `suspend`/`resume`.\n\n"
            "### Diferença das Threads\n"
            "| Aspecto | Thread | Coroutine |\n"
            "|---------|--------|----------|\n"
            "| Custo de criação | ~1 MB stack | ~1 KB heap |\n"
            "| Paralelismo | Preemptivo (SO) | Cooperativo |\n"
            "| Cancelamento | `interrupt()` frágil | Structured concurrency |\n\n"
            "### Exemplo\n"
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
            "Confundir `async`+`await` com paralelismo automático",
        ],
        study_suggestions=[
            "Kotlin Coroutines Guide — kotlinlang.org/docs/coroutines-guide.html",
            "Codelab 'Advanced Coroutines with Kotlin Flow' — developer.android.com",
            "Livro: 'Kotlin Coroutines Deep Dive' — Marcin Moskała",
            "Now in Android: exemplo real de coroutines em produção (GitHub)",
        ],
        confidence_score=0.88,
        processing_time_ms=int((time.time() - start) * 1000) + 180,
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
