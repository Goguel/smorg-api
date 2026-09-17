# Backlog de Histórias de Usuário - SmOrg API

Este diretório contém os detalhamentos completos das histórias de usuário (User Stories) e tarefas técnicas cadastradas no [GitHub Project #2 (Kanban)](https://github.com/users/Goguel/projects/2) e na aba [Issues](https://github.com/Goguel/smorg-api/issues).

---

## 📊 Matriz do Backlog

| # | Título | Tipo | Prioridade | Estimativa | Sprint | Status |
|:---:|---|:---:|:---:|:---:|:---:|:---:|
| [#1](https://github.com/Goguel/smorg-api/issues/1) | [CRUD de Equipamentos](./issue-1-crud-equipamentos.md) | User Story | **P1** | 5 pts | Sprint 1 | Pronto p/ Refinamento |
| [#2](https://github.com/Goguel/smorg-api/issues/2) | [Registrar Retirada de Equipamento](./issue-2-registrar-retirada.md) | User Story | **P1** | 5 pts | Sprint 1 | Pronto p/ Refinamento |
| [#3](https://github.com/Goguel/smorg-api/issues/3) | [Comunicação gRPC com Microsserviço Go](./issue-3-comunicar-com-go.md) | User Story | **P1** | 8 pts | Sprint 2 | Pronto p/ Refinamento |
| [#4](https://github.com/Goguel/smorg-api/issues/4) | [Registrar Devolução de Equipamento](./issue-4-registrar-devolucao.md) | User Story | **P1** | 3 pts | Sprint 1 | Pronto p/ Refinamento |
| [#5](https://github.com/Goguel/smorg-api/issues/5) | [Consultar Inventário de Itens](./issue-5-consultar-inventario.md) | User Story | **P2** | 3 pts | Sprint 1 | Pronto p/ Refinamento |
| [#6](https://github.com/Goguel/smorg-api/issues/6) | [Itens mais Requisitados em Cache](./issue-6-cache-itens-requisitados.md) | User Story | **P3** | 5 pts | Sprint 3 | Pronto p/ Refinamento |

---

## 🏷️ Convenção de Labels Recomendada

| Label | Cor | Descrição |
|---|:---:|---|
| `prio:P1` | `#B60205` | Alta prioridade (essencial para o MVP) |
| `prio:P2` | `#D93F0B` | Média prioridade (relevante para auditoria e gestão) |
| `prio:P3` | `#FBCA04` | Baixa prioridade / melhoria de performance |
| `sprint:sprint-1` | `#0E8A16` | Tarefas planejadas para a Sprint 1 |
| `sprint:sprint-2` | `#1D76DB` | Tarefas planejadas para a Sprint 2 |
| `sprint:sprint-3` | `#5319E7` | Tarefas planejadas para a Sprint 3 |
| `type:user-story` | `#0075CA` | História de Usuário centrada no valor de negócio |
| `type:tech-task` | `#A2EEEF` | Tarefa de infraestrutura, arquitetura ou CI/CD |
| `component:api-ktor` | `#F9D0C4` | Alterações no serviço principal (Kotlin / Ktor) |
| `component:svc-go` | `#C2E0C6` | Alterações no microsserviço de regras (Go) |
| `component:grpc` | `#D4C5F9` | Alterações em contratos gRPC e stubs |
| `component:database`| `#FEF2C0`| Migrações e persistência (PostgreSQL / Flyway) |
| `component:cache` | `#BFDADC` | Camada de cache e métricas de latência |

---

## 🎯 Milestones Sugeridas

1. **Sprint 1 — MVP Básico & Arquitetura Base**: CRUD de entidades relacionadas, migrações Flyway, Problem Details RFC 7807, Testcontainers e ArchUnit.
2. **Sprint 2 — Microsserviço Go & gRPC**: Comunicação gRPC síncrona com `PrazoService`, sincronia de stubs, testes ponta a ponta e arch-go compliance 100%.
3. **Sprint 3 — Cache, Banco Neon & Deploy**: Deploy em nuvem, métricas de cache hit/miss, health checks e logs estruturados.
4. **Sprint Final — Segurança OWASP & Apresentação**: Autenticação/autorização anti-BOLA, documentação de segurança OWASP e apresentação.
