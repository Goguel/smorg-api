---
name: "📖 História de Usuário (User Story)"
description: "Propor uma nova funcionalidade ou história de usuário para o SmOrg"
title: "[US]: "
labels: ["type:user-story"]
assignees: ["Goguel"]
---

## 👤 Descrição da História
- **Como** [tipo de usuário/persona, ex: Monitor, Aluno, Administrador, Sistema]
- **Quero** [executar uma ação / funcionalidade]
- **Para que** [obter um benefício ou resultado de negócio]

---

## 📋 Contexto e Regras de Negócio
<!-- Explicação do contexto, premissas e regras de negócio específicas -->
- 

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: [Cenário de Sucesso]
- **Dado** que ...
- **Quando** ...
- **Então** ...

### Cenário 2: [Cenário de Exceção/Erro]
- **Dado** que ...
- **Quando** ...
- **Então** ...

---

## 🛠️ Tarefas Técnicas (Checklist)
- [ ] Criar/atualizar migração Flyway
- [ ] Implementar entidades de domínio e validações
- [ ] Implementar repositório e persistência
- [ ] Expor endpoints REST no Ktor com Problem Details (RFC 7807)
- [ ] Documentar no OpenAPI/Swagger
- [ ] Testes unitários de domínio e casos de uso
- [ ] Testes de integração com Testcontainers (PostgreSQL)

---

## 📌 Metadados
- **Prioridade:** P1 / P2 / P3
- **Estimativa:** X pts
- **Sprint:** Sprint 1 / 2 / 3
