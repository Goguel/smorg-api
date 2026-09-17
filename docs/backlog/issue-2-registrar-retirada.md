# Issue #2: Registrar Retirada de Equipamento via NFC

- **Título Sugerido:** `[US02] Registrar retirada de equipamento via leitura NFC`
- **Tipo:** `type:user-story`
- **Prioridade:** `prio:P1`
- **Estimativa:** `5 pts`
- **Sprint:** `sprint:sprint-1`
- **Componentes:** `component:api-ktor`, `component:database`

---

## 👤 Descrição da História
- **Como** aluno devidamente cadastrado no laboratório
- **Quero** registrar a retirada de um equipamento através da leitura da tag NFC pelo aplicativo móvel
- **Para que** eu obtenha a permissão formal de uso e posse temporária do ativo com registro rastreável

---

## 📋 Contexto e Regras de Negócio

1. **Atrito Zero (NFC First):** O fluxo principal de retirada parte da aproximação do smartphone à tag NFC do equipamento físico catalogado. O aplicativo envia a `nfc_tag_id` e a identificação do aluno para a API.
2. **Disponibilidade do Item:** O equipamento só pode ser retirado se seu status atual for estritamente `DISPONIVEL`. Se já constar como `EMPRESTADO`, a requisição deve ser recusada com `409 Conflict`. Se estiver em `MANUTENCAO` ou `INATIVO`, retorna `422 Unprocessable Entity`.
3. **Validação de Inadimplência do Usuário (Bloqueio Transacional):** Alunos que possuam qualquer equipamento com devolução pendente em atraso ficam temporariamente bloqueados para novos empréstimos até a regularização da pendência.
4. **Limite de Empréstimos Simultâneos:** Cada aluno pode ter simultaneamente no máximo 3 equipamentos sob sua posse.
5. **Atomicidade da Operação:** A abertura do empréstimo (criação do registro na tabela `emprestimos`) e a mudança de status do equipamento para `EMPRESTADO` devem ocorrer na mesma transação de banco de dados (`ACID`), evitando inconsistências sob concorrência.
6. **Data Prevista de Devolução:** Na Sprint 1, define-se um prazo padrão de 7 dias corridos. Na Sprint 2, esse cálculo será delegado via gRPC ao microsserviço em Go.

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: Retirada de equipamento disponível com sucesso
- **Dado** que o aluno "Miguel" (ID `usr-01`) não possui atrasos pendentes
- **E** o equipamento "Osciloscópio" com tag `04A1B2C3D4` está com status `DISPONIVEL`
- **Quando** for enviada a requisição `POST /api/v1/emprestimos` com a tag e o ID do usuário
- **Então** a API deve responder com status `201 Created`
- **E** deve ser gerado um registro de empréstimo com `data_retirada` = timestamp atual
- **E** o status do equipamento deve ser alterado atomicamente para `EMPRESTADO`

### Cenário 2: Tentativa de retirada de equipamento já emprestado
- **Dado** que o equipamento associado à tag `04A1B2C3D4` já se encontra no status `EMPRESTADO`
- **Quando** outro aluno tentar efetuar a retirada do mesmo equipamento
- **Então** a API deve recusar a operação com status `409 Conflict`
- **E** o Problem Details (RFC 7807) deve informar que o equipamento já se encontra emprestado

### Cenário 3: Bloqueio de aluno com empréstimo em atraso
- **Dado** que o aluno possui um empréstimo cujo `data_devolucao_prevista` já expirou e o item não foi devolvido
- **Quando** o aluno tentar retirar um novo equipamento disponível
- **Então** a API deve recusar a operação com status `422 Unprocessable Entity`
- **E** o Problem Details deve indicar motivo de bloqueio por pendência em aberto

### Cenário 4: Tag NFC não reconhecida
- **Dado** que a tag NFC lida `FF99887766` não corresponde a nenhum equipamento cadastrado
- **Quando** for feita a tentativa de retirada
- **Então** a API deve responder com status `404 Not Found`

### Cenário 5: Concorrência em leituras simultâneas
- **Dado** que dois alunos tentam retirar o mesmo equipamento exatamente no mesmo instante
- **Quando** as duas requisições concorrentes chegarem à API
- **Então** exatamente uma das transações deve ser aprovada (201) e a outra deve ser rejeitada com `409 Conflict` por controle de concorrência

---

## 🔌 Contratos de API (Especificação REST)

### `POST /api/v1/emprestimos`
**Request Body:**
```json
{
  "usuario_id": "9e1c2b3a-5432-4f10-9876-0123456789ab",
  "nfc_tag_id": "04A1B2C3D4E5F6"
}
```

**Response (201 Created):**
```json
{
  "id": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
  "usuario_id": "9e1c2b3a-5432-4f10-9876-0123456789ab",
  "equipamento": {
    "id": "7b8f9e20-9426-4b2e-a34f-01584c6c1092",
    "nome": "Multímetro Digital Minipa ET-1002",
    "nfc_tag_id": "04A1B2C3D4E5F6",
    "status": "EMPRESTADO"
  },
  "data_retirada": "2026-09-16T14:30:00Z",
  "data_devolucao_prevista": "2026-09-23T14:30:00Z",
  "data_devolucao_real": null,
  "status_transacao": "ATIVA"
}
```

**Response (422 Unprocessable Entity - Usuário Inadimplente):**
```json
{
  "type": "https://smorg.ufrn.br/errors/usuario-bloqueado",
  "title": "Usuário Bloqueado para Empréstimos",
  "status": 422,
  "detail": "O usuário possui empréstimos com prazo vencido pendentes de devolução.",
  "instance": "/api/v1/emprestimos"
}
```

---

## 🛠️ Tarefas Técnicas de Implementação (Checklist)
- [ ] Criar migração Flyway `V2__create_table_usuarios_e_emprestimos.sql` com Foreign Keys e índices.
- [ ] Modelar agregados e entidades `Usuario`, `Emprestimo` e seus relacionamentos no Kotlin.
- [ ] Implementar serviço de verificação de elegibilidade do aluno (atrasos ativos e limite simultâneo).
- [ ] Implementar abertura atômica de empréstimo dentro de transação de banco com isolamento adequado (`Serializable` ou `SELECT ... FOR UPDATE`).
- [ ] Expor rota `POST /api/v1/emprestimos` no Ktor com validação e mapeamento para Problem Details.
- [ ] Documentar contrato no OpenAPI/Swagger.
- [ ] Criar testes de integração com Testcontainers simulando concorrência e cenários de bloqueio.

---

## 🏁 Definição de Pronto (Definition of Done - DoD)
- [ ] Relacionamento de entidades `Usuario`, `Equipamento` e `Emprestimo` persistido via PostgreSQL com Flyway.
- [ ] Lógica transacional de bloqueio de inadimplentes validada por testes automatizados.
- [ ] Testes de concorrência com Testcontainers verdes no CI.
- [ ] Cobertura de testes de integração no GitHub Actions.
