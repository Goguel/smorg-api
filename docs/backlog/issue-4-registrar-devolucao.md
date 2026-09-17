# Issue #4: Registrar Devolução de Equipamento via NFC

- **Título Sugerido:** `[US04] Registrar devolução de equipamento via leitura NFC`
- **Tipo:** `type:user-story`
- **Prioridade:** `prio:P1`
- **Estimativa:** `3 pts`
- **Sprint:** `sprint:sprint-1`
- **Componentes:** `component:api-ktor`, `component:database`

---

## 👤 Descrição da História
- **Como** aluno ou monitor do laboratório
- **Quero** registrar a devolução de um equipamento através da leitura física da tag NFC
- **Para que** a transação de empréstimo seja encerrada, a pendência seja baixada e o item volte a ficar disponível para outros colegas

---

## 📋 Contexto e Regras de Negócio

1. **Agilidade no Balcão de Devolução:** O processo de devolução não requer digitação de códigos de patrimônio; basta a leitura da tag NFC pelo smartphone do aluno ou monitor.
2. **Identificação do Empréstimo Ativo:** A API busca o registro de empréstimo em aberto (`data_devolucao_real IS NULL`) associado àquele equipamento (`nfc_tag_id`).
3. **Validação de Estado do Item:** Apenas equipamentos com status `EMPRESTADO` podem ter devolução registrada. Se o item já estiver `DISPONIVEL`, a API deve responder com erro de regra de negócio (`409 Conflict` ou `422 Unprocessable Entity`).
4. **Cálculo de Atraso:** A API compara `data_devolucao_real` (timestamp atual) com a `data_devolucao_prevista`. Se devolvido com atraso, registra a quantidade de dias em atraso e a flag `devolvido_com_atraso = true`.
5. **Transição Atômica de Estado:** A operação atualiza o empréstimo e altera o status do equipamento de volta para `DISPONIVEL` dentro da mesma transação de banco de dados.

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: Devolução dentro do prazo com sucesso
- **Dado** que o equipamento "Multímetro" (tag `04A1B2C3D4`) possui um empréstimo ativo com prazo previsto para o dia seguinte
- **Quando** for enviada a requisição `POST /api/v1/emprestimos/devolucao` com a tag NFC
- **Então** a API deve responder com status `200 OK`
- **E** o empréstimo deve ter sua `data_devolucao_real` preenchida
- **E** o status do equipamento deve retornar imediatamente para `DISPONIVEL`
- **E** a flag `devolvido_com_atraso` deve ser `false`

### Cenário 2: Devolução com atraso identificado
- **Dado** que o equipamento possui um empréstimo ativo cujo prazo previsto expirou há 2 dias
- **Quando** a devolução for registrada
- **Então** a API deve responder com status `200 OK`
- **E** a resposta deve sinalizar `devolvido_com_atraso = true` com a quantidade de dias de atraso calculada
- **E** o status do equipamento deve retornar para `DISPONIVEL`

### Cenário 3: Tentativa de devolução de equipamento que não está emprestado
- **Dado** que o equipamento já possui status `DISPONIVEL`
- **Quando** for solicitada a devolução via `POST /api/v1/emprestimos/devolucao`
- **Então** a API deve rejeitar a requisição com status `422 Unprocessable Entity`
- **E** o Problem Details deve informar que não há empréstimo ativo para o ativo informado

### Cenário 4: Leitura de Tag NFC inexistente
- **Dado** que a tag informada não existe na base de dados
- **Quando** for enviada a requisição de devolução
- **Então** a API deve retornar status `404 Not Found`

---

## 🔌 Contratos de API (Especificação REST)

### `POST /api/v1/emprestimos/devolucao`
**Request Body:**
```json
{
  "nfc_tag_id": "04A1B2C3D4E5F6"
}
```

**Response (200 OK - No Prazo):**
```json
{
  "emprestimo_id": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
  "equipamento": {
    "id": "7b8f9e20-9426-4b2e-a34f-01584c6c1092",
    "nome": "Multímetro Digital Minipa ET-1002",
    "status": "DISPONIVEL"
  },
  "usuario_id": "9e1c2b3a-5432-4f10-9876-0123456789ab",
  "data_retirada": "2026-09-16T14:30:00Z",
  "data_devolucao_prevista": "2026-09-23T14:30:00Z",
  "data_devolucao_real": "2026-09-18T09:15:00Z",
  "devolvido_com_atraso": false,
  "dias_atraso": 0
}
```

**Response (422 Unprocessable Entity - Item não emprestado):**
```json
{
  "type": "https://smorg.ufrn.br/errors/item-nao-emprestado",
  "title": "Item Não Emprestado",
  "status": 422,
  "detail": "O equipamento associado à tag NFC '04A1B2C3D4E5F6' não possui nenhum empréstimo ativo.",
  "instance": "/api/v1/emprestimos/devolucao"
}
```

---

## 🛠️ Tarefas Técnicas de Implementação (Checklist)
- [ ] Implementar o caso de uso `RegistrarDevolucaoUseCase` no serviço Ktor.
- [ ] Garantir atomicidade da atualização (fechamento do empréstimo + atualização do status do equipamento para `DISPONIVEL`).
- [ ] Calcular diferença entre `data_devolucao_real` e `data_devolucao_prevista`.
- [ ] Expor o endpoint `POST /api/v1/emprestimos/devolucao` no roteamento do Ktor.
- [ ] Mapear respostas de erro para Problem Details (RFC 7807).
- [ ] Atualizar documentação OpenAPI/Swagger.
- [ ] Escrever testes de integração com Testcontainers cobrindo devolução pontual e com atraso.

---

## 🏁 Definição de Pronto (Definition of Done - DoD)
- [ ] Endpoint de devolução funcional integrado ao fluxo de banco PostgreSQL.
- [ ] Resposta com status de atraso calculada corretamente.
- [ ] Testes de integração com Testcontainers passando no CI.
- [ ] Contrato documentado no OpenAPI.
