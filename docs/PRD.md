# TeseAI Live — Product Requirements Document (PRD)

## Visão Geral

TeseAI Live é um assistente multimodal acadêmico para simulação de entrevistas técnicas. Funciona como ferramenta de treinamento para desenvolvedores Android/Kotlin, captando texto, áudio e imagem e fornecendo respostas progressivas estruturadas.

## Limites Éticos (Inegociáveis)

1. **Não criar modo clandestino** — todos os modos são visíveis ao usuário
2. **Não criar captura oculta** — indicadores visuais obrigatórios quando câmera/microfone ativos
3. **Não incentivar uso em entrevistas reais** sem autorização dos avaliadores
4. **Onboarding ético obrigatório** — `EthicsConsentScreen` é a primeira tela
5. **API key nunca no app** — somente no backend

## Stack

| Camada     | Tecnologia                                      |
|------------|-------------------------------------------------|
| Frontend   | Android Kotlin, Jetpack Compose, Material 3     |
| DI         | Hilt                                            |
| Async      | Coroutines + StateFlow                          |
| Rede       | Retrofit + OkHttp                               |
| Câmera     | CameraX                                         |
| Persistência | DataStore, Room (opcional)                    |
| Backend    | FastAPI (Python 3.11+)                          |
| IA         | OpenAI API (somente backend)                   |
| Respostas  | Structured JSON                                 |

## Schema de Resposta /analyze/text

```json
{
  "session_id": "string — UUID da sessão",
  "detected_theme": "string — tema identificado",
  "quick_tip": "string — dica prática em 1-2 frases",
  "short_answer": "string — resposta objetiva",
  "interview_answer": "string — resposta ideal para entrevista (markdown)",
  "complete_answer": "string — explicação completa com código (markdown)",
  "common_errors": ["array de strings"],
  "study_suggestions": ["array de strings"],
  "confidence_score": 0.0,
  "processing_time_ms": 0,
  "mock": true
}
```

## Cards de Resposta (CockpitLiveScreen)

| Card               | Campo             | Ícone              |
|--------------------|-------------------|--------------------|
| Tema Detectado     | detected_theme    | Label              |
| Dica Rápida        | quick_tip         | Lightbulb          |
| Resposta Curta     | short_answer      | ShortText          |
| Resposta Entrevista| interview_answer  | RecordVoiceOver    |
| Resposta Completa  | complete_answer   | MenuBook           |
| Erros Comuns       | common_errors     | BugReport          |
| Sugestões de Estudo| study_suggestions | School             |

## Arquitetura Android (Clean)

```
presentation/
  screens/      → Composables (UI)
  viewmodel/    → ViewModel + UiState
domain/
  model/        → Entidades de domínio
data/
  remote/       → Retrofit API + DTOs
  repository/   → Repositórios (interface + impl)
di/             → Módulos Hilt
navigation/     → NavGraph
```

## Fases de Desenvolvimento

### Fase 0 — Fundação
- Estrutura do monorepo
- Gradle setup compilável
- Backend FastAPI base

### Fase 1 — Cockpit Live
- Todas as 6 telas (3 funcionais, 3 placeholder)
- Chamada /analyze/text → cards
- Modo mock por USE_MOCK_AI
- 11 testes no backend

### Fase 2 — Multimodal
- CameraX real com preview
- /analyze/image real
- Speech-to-Text
- Fluxo de entrevista completo

### Fase 3 — Polimento
- Room offline
- Relatórios persistidos
- PDF export
- Auth real
