# Backlog de Histórias de Usuário - SmOrg API

Este diretório contém os detalhamentos completos das histórias de usuário (User Stories) e tarefas técnicas cadastradas no [GitHub Project #2 (Kanban)](https://github.com/users/Goguel/projects/2) e na aba [Issues](https://github.com/Goguel/smorg-api/issues).

---

## 📊 Matriz do Backlog

| # | Título | Tipo | Prioridade | Estimativa | Sprint | Status |
|:---:|---|:---:|:---:|:---:|:---:|:---:|
| [#1](https://github.com/Goguel/smorg-api/issues/1) | [CRUD de Equipamentos](./issue-1-crud-equipamentos.md) | User Story | **P1** | 5 pts | Sprint 1 | Implementada ✅ |
| [#2](https://github.com/Goguel/smorg-api/issues/2) | [Registrar Retirada de Equipamento](./issue-2-registrar-retirada.md) | User Story | **P1** | 5 pts | Sprint 1 | Implementada ✅ |
| [#4](https://github.com/Goguel/smorg-api/issues/4) | [Registrar Devolução de Equipamento](./issue-4-registrar-devolucao.md) | User Story | **P1** | 3 pts | Sprint 1 | Implementada ✅ |
| [#5](https://github.com/Goguel/smorg-api/issues/5) | [Consultar Inventário e Empréstimos](./issue-5-consultar-inventario.md) | User Story | **P2** | 3 pts | Sprint 1 | Implementada ✅ |
| [#3](https://github.com/Goguel/smorg-api/issues/3) | [Comunicação gRPC com Microsserviço Go](./issue-3-comunicar-com-go.md) | User Story | **P1** | 8 pts | Sprint 2 | Planejada p/ Sprint 2 |
| [#6](https://github.com/Goguel/smorg-api/issues/6) | [Itens mais Requisitados em Cache](./issue-6-cache-itens-requisitados.md) | User Story | **P3** | 5 pts | Backlog | Opcional |

---

## 🏷️ Convenção de Labels Recomendada

| Label | Cor | Descrição |
|---|:---:|---|
| `prio:P1` | `#B60205` | Alta prioridade (essencial para o MVP) |
| `prio:P2` | `#D93F0B` | Média prioridade (relevante para auditoria e gestão) |
| `prio:P3` | `#FBCA04` | Baixa prioridade / melhoria de performance |
| `sprint:sprint-1` | `#0E8A16` | Tarefas da Sprint 1 |
| `sprint:sprint-2` | `#1D76DB` | Tarefas da Sprint 2 (Final) |
| `type:user-story` | `#0075CA` | História de Usuário centrada no valor de negócio |
| `type:tech-task` | `#A2EEEF` | Tarefa de infraestrutura, arquitetura ou CI/CD |
| `component:api-ktor` | `#F9D0C4` | Alterações no serviço principal (Kotlin / Ktor) |
| `component:svc-go` | `#C2E0C6` | Alterações no microsserviço de regras (Go) |
| `component:grpc` | `#D4C5F9` | Alterações em contratos gRPC e stubs |
| `component:database`| `#FEF2C0`| Migrações e persistência (PostgreSQL / Flyway) |
| `component:cache` | `#BFDADC` | Camada de cache e métricas de latência |

---

## 🎯 Milestones Atualizadas (Conforme Cronograma Oficial)

1. **Sprint 1 — MVP com CRUD de Entidades Relacionadas & Clean Architecture**:
   - $\ge 2$ entidades com relacionamento (`Equipamento` e `Emprestimo`).
   - Camadas desacopladas (Clean Architecture por pacotes com domínio puro).
   - Migrações Flyway versionadas (`V1` e `V2`).
   - Validações em DTOs, Problem Details (RFC 9457/7807) e OpenAPI interativo com Swagger UI (`/docs`).
   - Paginação e filtros semânticos.
2. **Sprint 2 (Entrega Final) — Microsserviço Go, gRPC e Sistema no Ar**:
   - Microsserviço Go idiomático com cálculo preciso de prazos de empréstimos.
   - Contrato Protobuf (`protos/`) com `buf lint` no CI e stubs gerados.
   - Integração gRPC síncrona com deadlines e resiliência na API Ktor.
   - Banco gerenciado Neon e API implantada com URL pública no README.
   - Suíte de testes completa: unitários, integração com banco real e testes de arquitetura (ArchUnit).

