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
