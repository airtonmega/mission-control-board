"""Banco de perguntas para simulação de entrevistas técnicas."""

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


def get_area_questions(area: str) -> list[str]:
    return _AREA_QUESTIONS.get(area, _AREA_QUESTIONS[_DEFAULT_AREA])
