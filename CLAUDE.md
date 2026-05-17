# CLAUDE.md — PostMortem AI
> Versão: 1.1.0 | Última atualização: 2026-05-16

---

## 🏗️ Visão Geral do Projeto

**PostMortem AI** é um gerador inteligente de post-mortems de incidentes a partir de logs e stack traces.

O usuário cola logs de produção, o sistema detecta automaticamente o formato, processa os dados e gera um post-mortem completo no padrão Google SRE — exportável em Markdown e PDF.

### Stack
- **Backend**: Java 21 + Spring Boot 3.2
- **Banco**: PostgreSQL 16 + Flyway
- **IA**: OpenAI API (`gpt-4o-mini`)
- **Frontend**: Angular 17
- **Testes**: JUnit 5 + Mockito + Testcontainers + WireMock
- **Deploy**: Render (backend) + Vercel (frontend)
- **CI/CD**: GitHub Actions

### Arquitetura
Clean Architecture com quatro camadas:
```
domain/          → entidades, value objects, interfaces de repositório
application/     → use cases, services, DTOs, portas de saída
infrastructure/  → repositórios JPA, OpenAI client, parsers, exportadores
presentation/    → controllers REST, exception handlers, mappers
```

---

## 📁 Estrutura de Pastas

```
postmortem-ai/
├── src/
│   └── main/
│       └── java/com/postmortemai/
│           ├── domain/
│           │   ├── model/          # Incident, PostMortem, LogEntry
│           │   ├── enums/          # Severity (P1-P4), LogFormat, IncidentStatus
│           │   └── repository/     # interfaces dos repositórios
│           ├── application/
│           │   ├── service/        # PostMortemService, LogProcessingService
│           │   ├── usecase/        # GeneratePostMortemUseCase
│           │   ├── dto/            # request/response DTOs
│           │   └── port/           # LogParser, PostMortemExporter, AIService (interfaces)
│           ├── infrastructure/
│           │   ├── parser/         # JsonLogParser, JavaStackTraceParser, PlainTextParser
│           │   │   └── detector/   # LogFormatDetector
│           │   ├── ai/             # OpenAIClient, ExtractionPromptBuilder, RedactionPromptBuilder
│           │   ├── export/         # MarkdownExporter, PdfExporter
│           │   ├── persistence/    # JPA entities, repositories
│           │   └── config/         # OpenAI config, beans
│           └── presentation/
│               ├── controller/     # PostMortemController, IncidentController
│               ├── handler/        # GlobalExceptionHandler
│               └── mapper/         # dto ↔ domain mappers
├── src/test/
│   ├── unit/                       # testes unitários por camada
│   └── integration/                # Testcontainers + WireMock
├── db/migration/                   # scripts Flyway (V1__, V2__...)
├── .github/workflows/              # CI/CD pipelines
└── CLAUDE.md
```

---

## ⚙️ Configurações do Ambiente

### Variáveis de ambiente obrigatórias
```
OPENAI_API_KEY=sk-...
DATABASE_URL=jdbc:postgresql://localhost:5432/postmortemai
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
```

### Versões
- Java 21 (LTS)
- Spring Boot 3.2.x
- PostgreSQL 16
- Node 20+ (frontend Angular)

### Setup local
```bash
docker-compose up -d        # sobe PostgreSQL
./mvnw spring-boot:run      # inicia backend
cd frontend && npm start    # inicia frontend
```

---

## 📐 Padrões do Projeto

### DTOs na fronteira
Toda comunicação entre `presentation` e `application` usa DTOs. O domínio nunca atravessa para fora da camada `application`.
```
LogInputRequestDTO → LogProcessingService → LogEntry (domain) → PostMortemResponseDTO
```

### Strategy Pattern para parsers
Cada formato de log é um parser separado implementando `LogParser`.
O `LogFormatDetector` detecta o formato e instancia o parser correto — o cliente nunca sabe qual parser está sendo usado.
```java
interface LogParser {
    boolean supports(LogFormat format);
    ParsedLog parse(String rawLog);
}
```

### Nomenclatura
- Classes de domínio: substantivos simples (`Incident`, `PostMortem`, `LogEntry`)
- Use cases: verbo + substantivo (`GeneratePostMortemUseCase`)
- Services: sufixo `Service` (`LogProcessingService`, `PostMortemService`)
- Parsers: formato + sufixo `Parser` (`JsonLogParser`, `JavaStackTraceParser`)
- DTOs: contexto + direção + sufixo (`LogInputRequestDTO`, `PostMortemResponseDTO`)

### Injeção de dependência
Sempre via construtor — nunca `@Autowired` em campo. Facilita testes e deixa dependências explícitas.

### Testes de integração com WireMock
Todo teste que chama a OpenAI API usa WireMock para simular respostas — nunca chama a API real nos testes.

---

## 🏛️ ADRs — Decisões de Arquitetura

### ADR-001: Detecção automática de formato com fallback manual
**Contexto**: O sistema precisa suportar três formatos de log distintos (JSON estruturado, Java stack trace, plain text).
**Decisão**: Detecção automática por heurística no `LogFormatDetector`. O usuário pode sobrescrever manualmente se a detecção falhar.
Heurísticas: `{` na primeira linha → JSON; presença de `Exception` + `at ` → stack trace Java; default → plain text.
**Consequências**: +UX zero-friction no happy path | -Logs híbridos (ex: stack trace dentro de JSON) exigem parser composto futuro
**Status**: Aceita

### ADR-002: Pipeline de dois prompts sequenciais
**Contexto**: Risco de alucinação — o LLM pode inventar dados que não existem nos logs.
**Decisão**: Pipeline separado em dois estágios:
- **Prompt 1 (extração)**: recebe o log processado, retorna JSON estruturado com fatos objetivos
- **Prompt 2 (redação)**: recebe *exclusivamente* o JSON de fatos, redige o post-mortem em linguagem natural
O modelo nunca redige a partir do log bruto.
**Consequências**: +Confiabilidade (sem alucinação estrutural) | +Testabilidade separada por estágio | -Duas chamadas à API por geração (custo irrelevante com gpt-4o-mini)
**Status**: Aceita

### ADR-003: Pre-processing obrigatório antes da chamada à API
**Contexto**: Logs reais podem ter dezenas de milhares de linhas. Jogar tudo no context window degrada qualidade ("lost in the middle" effect documentado pelo Zalando Engineering).
**Decisão**: Antes de chamar a API, o `LogPreProcessor` executa:
1. Deduplicação de linhas idênticas consecutivas
2. Remoção de linhas `DEBUG`/`TRACE` sem exception
3. Preservação garantida: primeiras e últimas N linhas + todas com `ERROR`/`FATAL`/`Exception`
4. Chunking com sliding window se o threshold de tokens for ultrapassado (default: 80k tokens)
**Consequências**: +Qualidade do output | +Redução de custo | -Complexidade adicional no parser
**Status**: Aceita

### ADR-004: OpenPDF + Flexmark para exportação PDF
**Contexto**: Necessidade de exportar post-mortems em PDF sem licença problemática.
**Decisão**: Markdown (gerado pelo Prompt 2) → HTML via Flexmark → PDF via OpenPDF.
Markdown é o formato primário — exportação MD vem de graça do pipeline.
**Alternativas descartadas**: iText 7 (AGPL, trava uso comercial), Apache PDFBox (API baixo nível, sem ganho de diferencial).
**Consequências**: +Licença limpa (LGPL) | +Markdown como cidadão de primeira classe | -Duas dependências de conversão
**Status**: Aceita

### ADR-005: Saída do LLM em JSON Schema com seções obrigatórias e opcionais
**Contexto**: Output em texto livre causa inconsistência de estrutura entre gerações.
**Decisão**: Prompt 1 retorna JSON estritamente tipado. Seções sem dados suficientes retornam `null` — nunca string inventada. A camada de exportação omite seções nulas do Markdown/PDF final.
**Consequências**: +Consistência | +Facilidade de exportação | -Prompt mais verboso (schema embutido)
**Status**: Aceita

### ADR-006: Idempotência via SHA-256 no GeneratePostMortemUseCase
**Contexto**: Logs idênticos submetidos múltiplas vezes gerariam chamadas desnecessárias à OpenAI, consumindo tokens e degradando performance.
**Decisão**: Antes de chamar o pipeline de IA, calcular SHA-256 do log bruto e verificar se já existe no banco. Se existir, retornar o post-mortem persistido sem chamar a API.
**Consequências**: +Economia de tokens | +Performance | +Idempotência garantida | -Hash precisa ser recalculado a cada request (custo negligenciável).
**Status**: Aceita

### ADR-007: Paginação agnóstica no domínio com PageQuery e PageResult
**Contexto**: Endpoint de histórico precisava de paginação sem vazar dependência do Spring Data para o domínio.
**Decisão**: Records PageQuery e PageResult criados na camada de aplicação. Barreira defensiva de size máximo de 100 itens por página.
**Consequências**: +Isolamento do domínio | +Testabilidade | -Uma camada extra de mapeamento.
**Status**: Aceita

### ADR-008: SUPPRESS_HTML no Flexmark para mitigação de XSS/SSRF
**Contexto**: Alucinações do LLM poderiam injetar tags HTML no Markdown gerado, criando vetor de XSS/SSRF na exportação PDF.
**Decisão**: Desabilitar renderização de HTML cru via HtmlRenderer.SUPPRESS_HTML no Flexmark.
**Consequências**: +Segurança sem regex adicional | +Zero risco de SSRF via alucinação do LLM.
**Status**: Aceita

### ADR-009: Dockerfile multi-stage com JRE 21 Alpine
**Contexto**: Imagem de produção precisava ser leve e segura, sem ferramentas de build expostas.
**Decisão**: Estágio de build com Maven Alpine (-DskipTests para evitar conflito com Testcontainers em CI sem daemon Docker). Estágio de runtime com JRE 21 Alpine apenas.
**Consequências**: +Imagem leve | +Superfície de ataque reduzida | -Testes não rodam no build da imagem (rodam no CI separado).
**Status**: Aceita

---

## 🐛 Pitfalls Conhecidos — Prevenção Obrigatória

### ⚠️ Surface Attribution Error (Zalando Engineering, 2025)
**O que é**: O LLM atribui causa raiz a uma tecnologia/componente simplesmente porque ele é *mencionado* no log — não porque causou o problema.
**Exemplo real**: Log menciona S3 em contexto de leitura normal. LLM classifica S3 como causa raiz do incidente mesmo sem evidência causal.
**Como prevenir**: O Prompt 1 deve conter instrução explícita:
```
"Only attribute root cause to components with direct causal evidence in the log sequence.
 Mentions alone are not causal evidence. If causality is ambiguous, set rootCause to null."
```
**Onde aplicar**: `ExtractionPromptBuilder` — verificar essa instrução antes de qualquer ajuste de prompt.

### ⚠️ "Lost in the Middle" Effect
**O que é**: Detalhes no meio de logs muito longos são ignorados ou distorcidos pelo modelo, mesmo dentro do context window.
**Como prevenir**: Pre-processing obrigatório (ADR-003) + chunking com overlap para logs acima de 80k tokens. Nunca jogar o log bruto completo no prompt.

### ⚠️ LLM retornando JSON malformado
**O que é**: O modelo ocasionalmente retorna JSON com campos extras, aspas escapadas incorretamente ou markdown code fences dentro do JSON.
**Como prevenir**: Sempre sanitizar a resposta antes de parsear:
1. Remover code fences (` ```json ` e ` ``` `)
2. Usar `ObjectMapper` com `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`
3. Envolver o parse em try-catch com fallback para resposta de erro estruturada — nunca propagar a exception raw para o controller.

### ⚠️ Interface Projection com LEFT JOIN para histórico paginado
**O que é**: Buscar entidades completas para o endpoint de histórico carregaria payloads pesados de log para memória desnecessariamente.
**Como prevenir**: Usar Interface Projection no repositório JPA com LEFT JOIN carregando apenas os campos necessários (id, title, severity, status, createdAt). Índice composto `idx_incidents_project_created` na migration Flyway.

---

## 🚀 Otimizações e Performance

*(seção populada durante o desenvolvimento)*

---

## 📐 Template do Post-Mortem — Seções

### Obrigatórias (sempre geradas)
| Campo | Descrição |
|-------|-----------|
| `title` | Nome descritivo do incidente |
| `severity` | P1 / P2 / P3 / P4 inferido pelo LLM |
| `status` | Resolved / Investigating / Monitoring |
| `summary` | Parágrafo executivo não-técnico (3-5 linhas) |
| `timeline` | Eventos ordenados por timestamp extraídos do log |
| `rootCause` | Causa raiz com evidência causal direta |
| `impact` | O que foi afetado e por quanto tempo |
| `detection` | Como/quando o problema foi detectado |

### Opcionais (geradas se dados suficientes, `null` caso contrário)
| Campo | Descrição |
|-------|-----------|
| `contributingFactors` | Fatores agravantes identificados |
| `actionItems` | Correções sugeridas com owner placeholder e prazo |
| `lessonsLearned` | O que o incidente revelou sobre o sistema |

---

## 🤖 Agentes — Mapeamento por Tarefa

| Tarefa | Agentes |
|--------|---------|
| Backend / API REST | `@engineering-backend-architect` + `@engineering-senior-developer` |
| Parser / log processing | `@engineering-data-engineer` + `@engineering-senior-developer` |
| Integração OpenAI | `@engineering-ai-engineer` + `@engineering-senior-developer` |
| Banco / migrations | `@engineering-database-optimizer` + `@engineering-senior-developer` |
| Testes | `@testing-api-tester` + `@testing-test-results-analyzer` |
| Code review | `@engineering-code-reviewer` |
| Frontend Angular | `@engineering-frontend-developer` |
| CI/CD | `@engineering-devops-automator` |
| Segurança | `@engineering-security-engineer` + `@engineering-backend-architect` |

### Sequência padrão para feature completa
`@engineering-backend-architect` → `@engineering-senior-developer` → `@testing-api-tester` → `@engineering-code-reviewer`

---

## 📚 Regras de Negócio

- Severidade (P1–P4) é **sempre inferida pelo LLM** — nunca pelo usuário no input
- Seções opcionais do post-mortem com dados insuficientes retornam `null` — a UI as omite
- O usuário **sempre revisa** o post-mortem antes de exportar — nunca publish automático
- Um post-mortem é sempre associado a um projeto/serviço — histórico é navegável por projeto
- Logs submetidos não são armazenados permanentemente — apenas o post-mortem gerado é persistido

---

## 🔗 Dependências Relevantes

| Dependência | Versão | Uso | Observação |
|-------------|--------|-----|------------|
| `spring-boot-starter-web` | 3.2.x | API REST | — |
| `spring-boot-starter-data-jpa` | 3.2.x | Persistência | — |
| `flyway-core` | 10.x | Migrations | — |
| `openai-java` (community) | latest | OpenAI client | Usar `OkHttpClient` com timeout configurável |
| `flexmark-all` | 0.64.x | Markdown → HTML | — |
| `openpdf` | 2.x | HTML → PDF | Licença LGPL — sem restrição comercial |
| `wiremock-standalone` | 3.x | Mock OpenAI nos testes | Apenas escopo `test` |
| `testcontainers-postgresql` | 1.19.x | PostgreSQL nos testes | — |
| `p6spy` | 3.x | Detecção N+1 em dev | Apenas perfil `dev` |

---

## 📝 Changelog do CLAUDE.md

| Versão | Data | O que mudou |
|--------|------|-------------|
| 1.1.0 | 2026-05-16 | Adiciona ADRs 006-009 e pitfall de Interface Projection com LEFT JOIN para otimização da Sprint 02 e 03 |
| 1.0.0 | 2026-05-16 | Versão inicial — ADRs 001-005, pitfalls do Zalando Engineering, estrutura completa do projeto |
