# PostMortem AI 🤖🛡️

[![Build Status](https://img.shields.io/badge/Build-Success-success.svg?style=flat-square&logo=github-actions)](https://github.com/joaogabriel43/PostMortem-AI)
[![Version](https://img.shields.io/badge/Version-v1.0.0-blue.svg?style=flat-square)](https://github.com/joaogabriel43/PostMortem-AI)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)

O **PostMortem AI** é uma plataforma inovadora baseada em SRE e Inteligência Artificial que automatiza o pipeline de análise de incidentes e gera relatórios de Post-Mortem profissionais a partir de logs brutos de produção (JSON, Plain Text e Stack Traces). O sistema adota rigorosamente os princípios da **Clean Architecture** (Arquitetura Hexagonal), **Resiliência**, **Observabilidade** e **Segurança Ativa**.

---

## 🌐 Fluxo Arquitetural & Topologia de Rede

O diagrama abaixo detalha a topologia de rede, incluindo o processamento assíncrono (SSE) e o stack de observabilidade:

```mermaid
graph TD
    %% Nodes
    Vercel["🌐 Vercel Client (Angular 17 Standalone)"]
    Render["☁️ Render Server (Spring Boot Backend)"]
    OpenAI["🤖 OpenAI API (gpt-4o-mini/structured-outputs)"]
    Postgres["🗄️ PostgreSQL Database"]
    Grafana["📊 Grafana (Dashboards)"]
    Prometheus["🔍 Prometheus (Metrics Scraper)"]
    
    %% Relationships with descriptive protocols
    Vercel -->|HTTPS / REST API / SSE (Server-Sent Events) / Rate Limited| Render
    Render -->|HTTPS / Circuit Breaker / Retry / Async| OpenAI
    Render -->|TCP / JPA / Caffeine Cache| Postgres
    Prometheus -->|HTTP GET /actuator/prometheus| Render
    Grafana -->|HTTP / PromQL| Prometheus
    
    %% Style adjustments
    classDef client fill:#121214,stroke:#45f3ff,stroke-width:2px,color:#fff;
    classDef server fill:#1f2833,stroke:#6f42c1,stroke-width:2px,color:#fff;
    classDef external fill:#0b0c10,stroke:#eab308,stroke-width:2px,color:#fff;
    classDef obs fill:#1e1e1e,stroke:#00d3ff,stroke-width:2px,color:#fff;
    
    class Vercel client;
    class Render,Postgres server;
    class OpenAI external;
    class Grafana,Prometheus obs;
```

---

## ⚡ Engenharia Premium & Desafios SRE Resolvidos

### 1. Assincronismo e UX em Tempo Real (SSE)
Chamadas para LLMs costumam gerar lentidão e timeouts, deteriorando a UX. Resolvemos esse gargalo implementando um **processamento totalmente assíncrono em background** gerenciado por um pool de threads dedicado (`@EnableAsync`).
O frontend não espera a IA: ele recebe um `202 Accepted` de forma imediata e estabelece uma conexão persistente via **Server-Sent Events (SSE)**. O Spring Boot "empurra" o status do progresso para o Angular nativamente (EventSource) até o completamento.

### 2. Rate Limiting, Prevenção a DDoS e Proteção de Custos
A integração com a OpenAI gera custos por tokens. Implementamos o **Bucket4j** como camada de segurança. O endpoint de geração bloqueia requisicões abusivas (limite de requests por minuto via IP ou Token), protegendo a plataforma financeiramente contra scrapers e exploits.

### 3. Observabilidade SRE (Prometheus & Grafana)
"Você não consegue melhorar aquilo que não mede". O Spring Actuator, aliado ao **Micrometer**, foi configurado para expor métricas customizadas de domínio (Counter para Taxas de Falhas de Geração e Timers para Latência da OpenAI). A stack do `docker-compose-obs.yml` provê Prometheus raspando a aplicação em tempo real e um dashboard do **Grafana** (`grafana-dashboard.json`) para acompanhamento de SLO/SLIs vitais da aplicação.

### 4. Cache em Memória com Caffeine
Para evitar consultas pesadas em rotas altamente frequentes (ex: histórico do projeto), instrumentamos um mecanismo nativo de **Spring Cache** impulsionado pela engine de alta perfomance **Caffeine**. Listagens estáticas e paginações não atingem o banco PostgreSQL desnecessariamente.

### 5. Idempotência Core via Hash SHA-256
Para cada entrada de logs brutos, geramos uma assinatura de integridade SHA-256 determinística. O pipeline faz uma consulta no banco de dados antes de invocar a OpenAI: se um log já existir, a plataforma realiza um short-circuit (curto-circuito) e retorna o relatório persistido.

### 6. Isolamento Hermético do Domínio (Clean Architecture)
Para impedir vazamentos de infraestrutura, os modelos de domínio puro `Incident` e `PostMortem` são simples Records Java imutáveis. Nenhum framework alcança o Core de Negócio. As persistências são desacopladas por meio de **Output Ports** (Interfaces). 

### 7. Proteção XSS (OWASP Bypass) e Segurança Binária
Geração de PDF usa **Flexmark** configurado estritamente com `HtmlRenderer.SUPPRESS_HTML = true`. Isso garante a neutralização de qualquer payload malicioso injetado nos logs antes de alimentar o motor **OpenPDF**.

---

## 🛠️ Tecnologias Utilizadas

### Backend SRE (Spring Boot 3.2.x):
* **Java 21 LTS** e **Spring Boot 3.2**
* **Bucket4j** (Proteção financeira e limite de requisições / Rate Limiting)
* **Caffeine & Spring Cache** (Otimização de busca em memória)
* **Micrometer, Prometheus & Grafana** (Observabilidade, telemetria SRE de latência e SLAs)
* **Resilience4j** (Circuit Breaker e Retry para tolerância a falhas na API externa)
* **Flexmark & OpenPDF** (Conversão segura de Markdown para HTML/PDF Binário sem risco de XSS)

### DevOps & Banco de Dados:
* **PostgreSQL** + **Flyway Migration**
* **Docker Compose** (Infraestrutura SRE isolada)

### Frontend (Angular 17 Standalone):
* **Angular Signals & Server-Sent Events (EventSource)**
* **HttpInterceptor & Glassmorphism CSS**

### Garantia de Qualidade Máxima (Testes):
* **Testcontainers & WireMock:** 67 testes de integração de ponta-a-ponta testando o fluxo contra bancos reais.
* **Pitest (Testes de Mutação):** Ferramenta que altera o bytecode deliberadamente no momento da compilação para certificar-se matematicamente de que os testes matam qualquer "mutante" e que não há falsos positivos na cobertura do código.

---

## 🚀 Como Executar Localmente

### Pré-requisitos
* Java 21+ instalado
* Node.js 18+ instalado
* Docker rodando localmente (necessário para os testes integrados e Prometheus)

### Executando o Backend
1. Clone o repositório e navegue até a pasta raiz:
   ```bash
   git clone https://github.com/joaogabriel43/PostMortem-AI.git
   cd PostMortem-AI
   ```
2. Forneça as credenciais necessárias no arquivo `.env`:
   ```bash
   cp .env.example .env
   ```
3. Suba o banco de dados PostgreSQL e a stack SRE (Prometheus + Grafana):
   ```bash
   docker-compose up -d
   docker-compose -f docker-compose-obs.yml up -d
   ```
4. Execute a aplicação Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
5. Para rodar a bateria rigorosa de testes unitários, E2E e **Pitest** de Mutação:
   ```bash
   ./mvnw clean verify pitest:mutationCoverage
   ```

### Executando o Frontend Angular
1. Navegue até a pasta `frontend/`:
   ```bash
   cd frontend
   ```
2. Instale dependências e inicie o servidor:
   ```bash
   npm install && npm start
   ```
3. Abra o navegador em [http://localhost:4200](http://localhost:4200). Para os Dashboards do Grafana, abra [http://localhost:3000](http://localhost:3000).

---
*Construído com excelência arquitetural e padrões enterprise-grade.*
