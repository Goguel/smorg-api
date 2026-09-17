# Issue #1: CRUD de Equipamentos com Identificação por Tag NFC

- **Título Sugerido:** `[US01] CRUD de equipamentos com identificação por Tag NFC`
- **Tipo:** `type:user-story`
- **Prioridade:** `prio:P1`
- **Estimativa:** `5 pts`
- **Sprint:** `sprint:sprint-1`
- **Componentes:** `component:api-ktor`, `component:database`

---

## 👤 Descrição da História
- **Como** monitor do laboratório
- **Quero** cadastrar, consultar, atualizar e inativar equipamentos vinculados às suas respectivas tags NFC físicas
- **Para que** os ativos estejam catalogados e aptos a serem emprestados de forma automatizada e sem atrito

---

## 📋 Contexto e Regras de Negócio

1. **Ativo Físico:** Cada equipamento representa um bem físico do laboratório (ex: multímetros digitais, osciloscópios, kits de desenvolvimento, fontes de bancada).
2. **Unicidade da Tag NFC:** A `nfc_tag_id` deve ser única em toda a base de dados. Caso já exista um equipamento ativo cadastrado com a mesma tag, a API deve rejeitar com `409 Conflict`.
3. **Status do Equipamento:** O ciclo de vida do equipamento segue o enum:
   - `DISPONIVEL` (status padrão ao ser cadastrado)
   - `EMPRESTADO` (ativo em posse de um aluno)
   - `MANUTENCAO` (temporariamente indisponível)
   - `INATIVO` (desativado do inventário)
4. **Restrições de Exclusão:** Um equipamento com status `EMPRESTADO` não pode ser removido nem inativado. A tentativa deve retornar `409 Conflict` ou `422 Unprocessable Entity`.
5. **Padronização de Erros:** Todas as respostas de erro de validação e regras de negócio devem seguir a RFC 7807 (**Problem Details**).

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: Cadastro de equipamento com sucesso
- **Dado** que o monitor envia um payload válido com `nome`, `nfc_tag_id` e `categoria`
- **E** a `nfc_tag_id` ainda não está cadastrada no sistema
- **Quando** a requisição `POST /api/v1/equipamentos` for processada
- **Então** a API deve responder com status `201 Created`
- **E** o cabeçalho `Location` deve apontar para `/api/v1/equipamentos/{id}`
- **E** o status inicial do equipamento deve ser `DISPONIVEL`

### Cenário 2: Erro ao cadastrar com tag NFC já existente
- **Dado** que já existe um equipamento cadastrado com a tag `04A1B2C3D4`
- **Quando** o monitor tenta cadastrar outro equipamento com a mesma `nfc_tag_id`
- **Então** a API deve responder com status `409 Conflict`
- **E** o corpo da resposta deve conter o Problem Details indicando o conflito no campo `nfc_tag_id`

### Cenário 3: Validação de campos obrigatórios
- **Dado** que o monitor envia um payload com `nome` em branco ou `nfc_tag_id` ausente
- **Quando** a requisição `POST /api/v1/equipamentos` for processada
- **Então** a API deve responder com status `400 Bad Request`
- **E** o Problem Details deve listar os campos inválidos e seus respectivos motivos

### Cenário 4: Consulta de equipamento por ID e por Tag NFC
- **Dado** que existe um equipamento cadastrado com ID `123e4567-e89b-12d3-a456-426614174000` e tag `04A1B2C3D4`
- **Quando** for realizada uma requisição `GET /api/v1/equipamentos/{id}` ou `GET /api/v1/equipamentos/tag/{nfcTagId}`
- **Então** a API deve retornar status `200 OK` com os detalhes completos do equipamento

### Cenário 5: Exclusão lógica de equipamento disponível
- **Dado** que existe um equipamento cadastrado com status `DISPONIVEL`
- **Quando** o monitor requisitar `DELETE /api/v1/equipamentos/{id}`
- **Então** a API deve responder com status `204 No Content`
- **E** o equipamento deve passar para `INATIVO` ou ser removido do acervo consultável

---

## 🔌 Contratos de API (Especificação REST)

### `POST /api/v1/equipamentos`
**Request Body:**
```json
{
  "nome": "Multímetro Digital Minipa ET-1002",
  "descricao": "Multímetro de bancada para laboratório de eletrônica",
  "nfc_tag_id": "04A1B2C3D4E5F6",
  "categoria": "ELETRONICA"
}
```

**Response (201 Created):**
```json
{
  "id": "7b8f9e20-9426-4b2e-a34f-01584c6c1092",
  "nome": "Multímetro Digital Minipa ET-1002",
  "descricao": "Multímetro de bancada para laboratório de eletrônica",
  "nfc_tag_id": "04A1B2C3D4E5F6",
  "categoria": "ELETRONICA",
  "status": "DISPONIVEL",
  "criado_em": "2026-09-16T10:00:00Z"
}
```

**Response (409 Conflict - Problem Details RFC 7807):**
```json
{
  "type": "https://smorg.ufrn.br/errors/conflito-tag-nfc",
  "title": "Conflito de Tag NFC",
  "status": 409,
  "detail": "Já existe um equipamento cadastrado com a tag NFC '04A1B2C3D4E5F6'.",
  "instance": "/api/v1/equipamentos"
}
```

---

## 🛠️ Tarefas Técnicas de Implementação (Checklist)
- [ ] Criar migração Flyway `V1__create_table_equipamentos.sql` com índices para `nfc_tag_id`.
- [ ] Modelar entidade de domínio `Equipamento` e Value Objects associados em Kotlin.
- [ ] Implementar repositório com Exposed/PostgreSQL com verificação de unicidade.
- [ ] Implementar serviço de aplicação com validação de payload (validação fail-fast).
- [ ] Configurar rotas REST no Ktor (`/api/v1/equipamentos`).
- [ ] Implementar plugin/interceptador de tratamento de exceções gerando Problem Details (RFC 7807).
- [ ] Documentar rotas e schemas com anotações OpenAPI/Swagger.
- [ ] Escrever testes de integração com Testcontainers (PostgreSQL) para todos os cenários de aceitação.
- [ ] Validar conformidade de arquitetura com ArchUnit (respeito às camadas de dependência).

---

## 🏁 Definição de Pronto (Definition of Done - DoD)
- [ ] CRUD completo funcional e documentado no OpenAPI.
- [ ] Migração Flyway executando sem erros no startup.
- [ ] Problem Details ativo para erros 400, 404, 409 e 500.
- [ ] Testes de integração cobrindo 100% dos cenários de aceitação passando localmente e no GitHub Actions.
- [ ] Teste de arquitetura ArchUnit verde no CI.
