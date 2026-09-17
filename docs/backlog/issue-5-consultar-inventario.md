# Issue #5: Consultar Inventário de Itens e Auditoria de Empréstimos

- **Título Sugerido:** `[US05] Consultar inventário de equipamentos e auditoria de empréstimos`
- **Tipo:** `type:user-story`
- **Prioridade:** `prio:P2`
- **Estimativa:** `3 pts`
- **Sprint:** `sprint:sprint-1`
- **Componentes:** `component:api-ktor`, `component:database`

---

## 👤 Descrição da História
- **Como** administrador ou monitor do laboratório
- **Quero** consultar o inventário de equipamentos aplicando filtros por status, categoria e histórico de movimentações
- **Para que** eu possa auditar o acervo do laboratório, identificar materiais em posse de alunos e localizar empréstimos em atraso

---

## 📋 Contexto e Regras de Negócio

1. **Visibilidade Gerencial:** Além da operação rápida de ponta via NFC, os gestores do laboratório precisam de visibilidade do acervo para planejamento, auditoria e cobrança de devoluções.
2. **Filtros Flexíveis de Inventário:** A listagem de equipamentos deve permitir filtrar por:
   - `status`: `DISPONIVEL`, `EMPRESTADO`, `MANUTENCAO`, `INATIVO`.
   - `categoria`: ex. `ELETRONICA`, `INFORMATICA`, `FERRAMENTA`.
   - `busca`: termo textual para casar com nome ou tag NFC.
3. **Auditoria de Empréstimos Ativos:** Deve haver um endpoint para listar os empréstimos ativos com indicação clara de:
   - Identificação do aluno responsável.
   - Equipamento em posse.
   - Data de retirada e data prevista de devolução.
   - Indicador de se o empréstimo já se encontra em atraso (`em_atraso: true`).
4. **Paginação Eficiente:** A listagem deve suportar parâmetros de paginação (`page` default 0, `size` default 20) para evitar sobrecarga de memória na API e no banco.

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: Filtrar equipamentos por status "EMPRESTADO"
- **Dado** que existem 10 equipamentos cadastrados, sendo 3 emprestados e 7 disponíveis
- **Quando** o administrador requisitar `GET /api/v1/equipamentos?status=EMPRESTADO`
- **Então** a API deve responder com status `200 OK`
- **E** a lista retornada deve conter exatamente os 3 equipamentos com status `EMPRESTADO`

### Cenário 2: Consultar empréstimos ativos com filtro de atraso
- **Dado** que há empréstimos ativos cuja `data_devolucao_prevista` já expirou
- **Quando** for realizada a requisição `GET /api/v1/emprestimos?status=ATIVO&atrasados=true`
- **Então** a API deve responder com `200 OK`
- **E** listar os empréstimos em atraso ordenados pelo maior tempo de atraso

### Cenário 3: Histórico de empréstimos de um equipamento
- **Dado** um equipamento com ID `7b8f9e20-9426-4b2e-a34f-01584c6c1092`
- **Quando** o monitor requisitar `GET /api/v1/equipamentos/{id}/historico`
- **Então** a API deve retornar a lista cronológica de todos os empréstimos já realizados para aquele ativo

---

## 🔌 Contratos de API (Especificação REST)

### `GET /api/v1/equipamentos?status=EMPRESTADO&page=0&size=10`
**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "7b8f9e20-9426-4b2e-a34f-01584c6c1092",
      "nome": "Osciloscópio Digital Tektronix TBS1052B",
      "nfc_tag_id": "04A1B2C3D4E5F6",
      "categoria": "ELETRONICA",
      "status": "EMPRESTADO"
    }
  ],
  "page": 0,
  "size": 10,
  "total_elements": 1,
  "total_pages": 1
}
```

### `GET /api/v1/emprestimos/ativos?atrasados=true`
**Response (200 OK):**
```json
[
  {
    "emprestimo_id": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
    "usuario": {
      "id": "9e1c2b3a-5432-4f10-9876-0123456789ab",
      "nome": "Aluno Teste",
      "matricula": "20240012345"
    },
    "equipamento": {
      "id": "7b8f9e20-9426-4b2e-a34f-01584c6c1092",
      "nome": "Osciloscópio Digital Tektronix TBS1052B"
    },
    "data_retirada": "2026-09-01T10:00:00Z",
    "data_devolucao_prevista": "2026-09-08T23:59:59Z",
    "em_atraso": true,
    "dias_em_atraso": 8
  }
]
```

---

## 🛠️ Tarefas Técnicas de Implementação (Checklist)
- [ ] Criar consultas com filtros dinâmicos e paginação no repositório Exposed/PostgreSQL.
- [ ] Criar índices no banco de dados para `status` e `data_devolucao_prevista` nas migrações Flyway.
- [ ] Implementar rota `GET /api/v1/equipamentos` com suporte a query parameters.
- [ ] Implementar rota `GET /api/v1/emprestimos/ativos` com filtro de atraso.
- [ ] Implementar rota `GET /api/v1/equipamentos/{id}/historico`.
- [ ] Documentar parâmetros e modelos de paginação no OpenAPI/Swagger.
- [ ] Testes com Testcontainers cobrindo consultas filtradas e paginação.

---

## 🏁 Definição de Pronto (Definition of Done - DoD)
- [ ] Endpoints de inventário e auditoria retornando dados paginados e filtrados.
- [ ] Documentação completa no OpenAPI.
- [ ] Testes de integração cobrindo múltiplos filtros passando no CI.
