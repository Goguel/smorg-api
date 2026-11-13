# Registro de Uso de Ferramentas de Inteligência Artificial — SmOrg

Conforme estabelecido em `AVALIACAO.md §9`, este documento registra as ferramentas de IA utilizadas no desenvolvimento do projeto, os contextos de uso e a validação humana realizada.

---

## Sprint 0
* **Ferramenta:** Antigravity (Google DeepMind)
* **Finalidade:**
  - Auxílio na estruturação inicial dos arquivos de monorepo (`mise.toml`, `docker-compose.yml`, `ci.yml`).
  - Revisão ortográfica e formatação da proposta de arquitetura inicial.
* **Supervisão Humana:** Miguel Xavier de Morais. Todo o código gerado foi revisado, testado e validado localmente.

---

## Sprint 1
* **Ferramenta:** Antigravity (Google DeepMind)
* **Finalidade:**
  - Apoio na modelagem do domínio em Clean Architecture (`Equipamento` e `Emprestimo`) e DTOs de entrada.
  - Implementação dos adaptadores de banco com Exposed e elaboração das migrações Flyway versionadas (`V1` e `V2`).
  - Configuração do plugin `StatusPages` com especificação RFC 9457 (Problem Details) e geração de documentação Swagger UI / OpenAPI.
  - Escrita dos testes unitários de aceitação em memória cobrindo os fluxos de sucesso e exceções de negócio.
* **Supervisão Humana:** Miguel Xavier de Morais. A lógica de negócios, status HTTP, regras de integridade relacional e conformidade arquitetural foram analisadas e validadas.

---

## Sprint 2
* **Ferramenta:** Antigravity (Google DeepMind)
* **Finalidade:**
  - Análise dos requisitos, rubricas e cronograma da Sprint 2 com base nos documentos de referência do professor.
  - Estruturação do contrato Protocol Buffers (`prazos.proto`) com governança Buf CLI (`buf.yaml` e `buf.gen.yaml`).
  - Modelagem do domínio Go puro do microsserviço de prazos (`services/prazos/`) com suporte a `context.Context` e testes unitários.
  - Implementação do servidor gRPC em Go (`services/grpcapi/` e `services/cmd/prazos/`) com health check (`grpc.health.v1`), reflection, graceful stop e testes em memória com `bufconn`.
  - Configuração do plugin Protobuf e stubs Kotlin no Gradle (`api/build.gradle.kts`) e implementação do cliente gRPC resiliente (`CalculadorDePrazosGrpc`) com deadline e fallback de contingência.
  - Elaboração das decisões arquiteturais: ADR 001 (Microsserviço Go) e ADR 002 (Estratégia de Implantação e Nuvem).
  - Escrita dos testes de arquitetura com ArchUnit (`ArquiteturaTest.kt`) e testes de integração com banco real via Testcontainers (`BancoIntegrationTest.kt`).
  - Configuração dos Dockerfiles multi-stage, `docker-compose.yml` com health checks de todos os serviços, licença MIT (`LICENSE`) e ampliação do GitHub Actions CI.
* **Supervisão Humana:** Miguel Xavier de Morais. A conformidade com o calendário acadêmico da UFRN, a regra dos dias úteis, o tratamento de contingência na API Ktor e a política de segredos fora do repositório foram analisadas e validadas.

