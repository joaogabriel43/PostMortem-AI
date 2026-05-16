# PostMortem AI

![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)
![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

Uma plataforma white-label baseada em Inteligência Artificial para processamento, higienização e geração automática de relatórios de Post-Mortem a partir de logs brutos de incidentes.

## Status do Projeto

✅ **Fase 1 Encerrada:** A Sprint 01 (Fundação e Pipeline Core) foi finalizada com absoluto sucesso. O repositório encontra-se pronto, estável e com arquitetura consolidada para a inicialização da Sprint 02.

Detalhes completos da Sprint 01 e decisões arquiteturais podem ser encontrados no [Walkthrough Técnico da Sprint 01](./walkthrough.md).

## Arquitetura Física e Stack Tecnológica

O projeto segue estritamente os princípios de **Clean Architecture**, dividindo responsabilidades e isolando o domínio de negócio de infraestruturas voláteis.

**Tecnologias Core:**
- **Linguagem & Framework:** Java 21, Spring Boot 3.2.x
- **Persistência de Dados:** PostgreSQL 16 (porta 5434)
- **Migrations:** Flyway 9
- **Resiliência e Integração (IA):** OpenAI API com Circuit Breaker e Retry implementados via Resilience4j
- **Testes Automatizados:** JUnit 5, Mockito, Testcontainers, WireMock
- **Monitoramento de Banco:** p6spy (interceptador de queries)

### Estrutura de Pacotes (Clean Architecture)

A divisão de pacotes respeita as regras de dependência (de fora para dentro):

- `domain/`: Contém os modelos puros (imutáveis via Records, ex: `Incident`, `PostMortem`) e enums. Nenhuma anotação de banco de dados (`@Entity`) ou frameworks entra aqui.
- `application/`: Abriga as portas de entrada e saída (`Ports`), casos de uso orquestradores (ex: `GeneratePostMortemUseCase`), e contratos de DTO internos.
- `infrastructure/`: Implementação técnica dos adaptadores (banco de dados via Spring Data JPA, clientes HTTP resilientes da OpenAI, parsers de log, e migrações Flyway). Aqui residem os `*RepositoryAdapter` que fazem a tradução entre as entidades JPA e os modelos de domínio puro.
- `presentation/`: Camada externa responsável pelas interfaces de comunicação, contendo os Controladores REST (`IncidentController`), DTOs de Request/Response e o tratamento padronizado de exceções (`GlobalExceptionHandler` seguindo a RFC 7807 ProblemDetail).

## Guia de Execução Local

### Pré-requisitos
- **Java 21** e **Maven** instalados localmente.
- **Docker** e **Docker Compose** configurados para subir o banco de dados.
- Uma chave válida de API da OpenAI.

### Passos de Inicialização

1. **Configuração de Ambiente:**
   Copie o arquivo de variáveis de ambiente de exemplo e insira sua chave da OpenAI (sem comitar credenciais reais):
   ```bash
   cp .env.example .env
   # Edite o arquivo .env e defina a variável OPENAI_API_KEY=${SUA_CHAVE_AQUI}
   ```

2. **Subir a Infraestrutura:**
   O banco de dados PostgreSQL parametrizado para o perfil de desenvolvimento rodará na porta `5434`.
   ```bash
   docker-compose up -d
   ```

3. **Rodar a Aplicação:**
   Execute a aplicação em perfil `dev` para habilitar o interceptador de queries do banco (p6spy) e rodar as migrações automaticamente:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Testes e Confiabilidade

O projeto conta com mais de 50 testes automatizados (unitários e de integração), mantendo uma cobertura de resiliência e processamento de dados em 100%. A suíte `E2E` integra banco real via Testcontainers e mock da API da OpenAI via WireMock.

Para executar localmente toda a suíte de testes do projeto:
```bash
./mvnw clean test
```

## Licença
Este projeto é licenciado sob a licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.
