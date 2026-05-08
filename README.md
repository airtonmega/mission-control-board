# TeseAI Live

Assistente multimodal acadêmico para simulação de entrevistas técnicas, treinamento, acessibilidade e demonstrações autorizadas.

**Aviso ético:** Este projeto destina-se exclusivamente a pesquisa acadêmica de mestrado e treinamento. **Não deve ser usado em entrevistas reais, provas ou avaliações sem consentimento explícito dos avaliadores.**

---

## Estrutura do Monorepo

```
teseai-live/
├── android-app/          # App Android (Kotlin + Jetpack Compose)
├── backend/              # API FastAPI (Python)
├── docs/                 # Documentação do projeto
├── .env.example          # Variáveis de ambiente de exemplo
└── README.md
```

---

## Backend (FastAPI)

### Pré-requisitos
- Python 3.11+
- pip

### Instalação e execução

```bash
cd backend
cp .env.example .env
# Edite .env se quiser usar a OpenAI API real (USE_MOCK_AI=false + OPENAI_API_KEY=sk-...)

pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

A API estará disponível em `http://localhost:8000`.

- Docs interativos: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`

### Docker

```bash
cd backend
docker compose up --build
```

### Testes

```bash
cd backend
python -m pytest tests/ -v
```

### Endpoints disponíveis

| Método | Endpoint                        | Descrição                      |
|--------|---------------------------------|--------------------------------|
| GET    | `/health`                       | Health check da API            |
| POST   | `/auth/anonymous`               | Cria sessão anônima + token    |
| POST   | `/consent/accept`               | Registra consentimento ético   |
| POST   | `/analyze/text`                 | Analisa pergunta técnica (IA)  |
| POST   | `/analyze/image`                | Analisa imagem (mock Fase 2)   |
| POST   | `/interview/start`              | Inicia simulação de entrevista |
| POST   | `/interview/evaluate`           | Avalia resposta de entrevista  |
| GET    | `/reports/session/{session_id}` | Relatório de sessão            |

### Schema de resposta `/analyze/text`

```json
{
  "session_id": "uuid",
  "detected_theme": "Kotlin — Coroutines & Concorrência",
  "quick_tip": "Use structured concurrency...",
  "short_answer": "...",
  "interview_answer": "...",
  "complete_answer": "...",
  "common_errors": ["...", "..."],
  "study_suggestions": ["...", "..."],
  "confidence_score": 0.82,
  "processing_time_ms": 230,
  "mock": true
}
```

### Modo Mock

Com `USE_MOCK_AI=true` (padrão), nenhuma chamada à OpenAI é feita. Ideal para desenvolvimento e demonstrações sem custo.

Com `USE_MOCK_AI=false` e `OPENAI_API_KEY` preenchida, o backend usa o `OpenAIService` real:
- Prompt carregado de `prompts/system_live_assistant.md`
- Resposta validada com Pydantic (`OpenAIRawResponse`)
- Se inválida: 1 tentativa de reparo automático
- Se reparo falhar: erro estruturado retornado ao Android
- Latência registrada por request nos logs

---

## Android App

### Pré-requisitos
- Android Studio Ladybug (2024.2+) ou superior
- JDK 17
- Android SDK (compileSdk 35, minSdk 26)

### Configuração

```bash
cd android-app
# O arquivo local.properties é gerado automaticamente pelo Android Studio
# Certifique-se que sdk.dir aponta para seu Android SDK
```

### Executar

1. Abra `android-app/` no Android Studio
2. Aguarde o sync do Gradle
3. Execute em emulador ou dispositivo físico

> **Backend no emulador:** Use `http://10.0.2.2:8000` para acessar o backend rodando na máquina host a partir do emulador Android.

### Telas implementadas (Fase 1)

| Tela                    | Descrição                                          |
|-------------------------|----------------------------------------------------|
| `EthicsConsentScreen`   | Onboarding ético — aceite obrigatório              |
| `CockpitLiveScreen`     | Dashboard principal com análise de texto + cards   |
| `CameraAnalysisScreen`  | Placeholder — análise por câmera (Fase 2)          |
| `InterviewSetupScreen`  | Placeholder — simulação de entrevistas (Fase 2)    |
| `ReportsScreen`         | Placeholder — relatórios de sessão (Fase 2)        |
| `SettingsScreen`        | Configurações (modo mock, URL da API, permissões)  |

### Stack Android

- Kotlin + Coroutines + StateFlow
- Jetpack Compose + Material 3
- Hilt (injeção de dependência)
- Retrofit + OkHttp (rede)
- CameraX (integrado — Fase 2)
- DataStore Preferences

---

## Segurança

- A OpenAI API key **nunca** fica no app Android — apenas no backend
- O backend usa variáveis de ambiente (`.env`)
- Indicadores visuais mostram quando câmera/microfone estão ativos
- Consentimento ético obrigatório no primeiro uso

---

## Fases do Projeto

### Fase 0 — Estrutura (concluída)
- Monorepo criado
- Backend FastAPI operacional
- App Android compilável

### Fase 1 — Cockpit Live (concluída)
- EthicsConsentScreen com aceite obrigatório
- CockpitLiveScreen com análise de texto
- 7 cards de resposta (tema, dica, curta, entrevista, completa, erros, estudo)
- Modo mock sem API key
- 11 testes no backend (100% passando)

### Fase 2 — OpenAI Real + Tratamento de Erros (concluída)
- `OpenAIService` isolado em `app/services/openai_service.py`
- System prompt dedicado em `prompts/system_live_assistant.md`
- Validação Pydantic com `OpenAIRawResponse` (schema interno separado do contrato público)
- Repair loop: 1 tentativa automática se resposta inválida, com prompt de correção dirigido
- 3 tipos de erro mapeados: `AI_RESPONSE_INVALID` (422), `AI_SERVICE_UNAVAILABLE` (503), `INTERNAL_ERROR` (500)
- Erros retornados como JSON estruturado `{"detail": {"error_code": ..., "error_message": ..., ...}}`
- Android parseia `ApiErrorEnvelope` → exibe `error_message` em PT-BR ao usuário
- Latência registrada em log estruturado a cada request
- 26 testes no backend (100% passando)
- Fallback mock automático se `USE_MOCK_AI=true` ou `OPENAI_API_KEY` ausente

### Fase 3 — Multimodal (próximos passos)
- [ ] CameraX integrado com análise de imagem real (`/analyze/image` real)
- [ ] Speech-to-Text para entrada por voz
- [ ] Indicadores de câmera/microfone ao vivo (permissão real)
- [ ] Fluxo completo de simulação de entrevista
- [ ] Relatórios de sessão persistidos

### Fase 4 — Polimento (roadmap)
- [ ] Room para histórico offline
- [ ] Export de relatório em PDF
- [ ] Auth real com Google Sign-In
- [ ] Testes instrumentados Android

### Fase 3 — Polimento (roadmap)
- [ ] Room para histórico offline
- [ ] Export de relatório em PDF
- [ ] Autenticação por conta (Google Sign-In)
- [ ] Suporte a múltiplos idiomas
- [ ] Testes instrumentados no Android

---

## Contribuição

Este projeto é parte de uma tese de mestrado. Contribuições são bem-vindas via Pull Request seguindo os princípios éticos documentados na `EthicsConsentScreen`.

---

*TeseAI Live — Pesquisa Acadêmica de Mestrado*
