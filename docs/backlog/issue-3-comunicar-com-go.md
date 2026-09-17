# Issue #3: Integração gRPC entre Ktor e Microsserviço Go para Cálculo de Prazos

- **Título Sugerido:** `[US03] Comunicação gRPC síncrona com microsserviço Go para cálculo de prazos`
- **Tipo:** `type:user-story`
- **Prioridade:** `prio:P1`
- **Estimativa:** `8 pts`
- **Sprint:** `sprint:sprint-2`
- **Componentes:** `component:api-ktor`, `component:svc-go`, `component:grpc`

---

## 👤 Descrição da História
- **Como** sistema Ktor (API Principal)
- **Quero** realizar uma chamada gRPC síncrona para o microsserviço em Go no momento da abertura do empréstimo
- **Para que** a data limite de devolução seja calculada com precisão por um motor de regras isolado, considerando o perfil do usuário e o calendário acadêmico (descontando fins de semana)

---

## 📋 Contexto e Regras de Negócio

1. **Separação Arquitetural Poliglota:** O microsserviço Go atua como "Motor de Regras e Prazos". Em vez de acoplar regras de negócio de calendário e perfis acadêmicos mutáveis no Ktor, essa lógica fica isolada no serviço Go, com comunicação de altíssima performance via gRPC + HTTP/2.
2. **Contrato Protobuf Oficial (`protos/prazos.proto`):**
   - Pacote: `smorg.prazos.v1`
   - Serviço: `PrazoService`
   - Método: `rpc CalcularPrazoDevolucao (CalcularPrazoRequest) returns (CalcularPrazoResponse);`
3. **Regras de Negócio de Prazos (Implementadas no serviço Go):**
   - **ALUNO:** 3 dias úteis.
   - **MONITOR:** 7 dias úteis.
   - **PROFESSOR:** 15 dias úteis.
   - Fins de semana (sábados e domingos) e feriados cadastrados **não contam** como dias úteis. A data final sempre expira às 23:59:59 UTC do último dia útil concedido.
4. **Governança do Contrato (Buf):** Os arquivos `.proto` devem passar por `buf lint` e `buf breaking` no pipeline de CI para garantir retrocompatibilidade.
5. **Resiliência e Timeout:** O cliente gRPC no Ktor deve aplicar timeout estrito (ex: 2000ms) e política de fallback/erro com Problem Details (RFC 7807) caso o microsserviço esteja inacessível.

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: Cálculo correto de prazo ignorando fins de semana para Aluno
- **Dado** que um aluno retira um equipamento em uma quinta-feira às 10:00
- **E** a regra para aluno concede 3 dias úteis
- **Quando** o Ktor invocar `CalcularPrazoDevolucao` no serviço Go
- **Então** o serviço Go deve retornar `dias_concedidos = 3`
- **E** a `data_devolucao_prevista` deve cair exatamente na terça-feira subsequente (pulando sábado e domingo)

### Cenário 2: Cálculo diferenciado para monitor e professor
- **Dado** que o empréstimo é solicitado por um monitor ou professor
- **Quando** a requisição for processada pelo serviço Go
- **Então** o prazo concedido deve refletir a respectiva regra (7 dias úteis para monitor, 15 dias úteis para professor)

### Cenário 3: Validação de Lint e Breaking Changes com Buf
- **Dado** uma alteração nos arquivos da pasta `protos/`
- **Quando** o pipeline de CI rodar `buf lint` e `buf breaking`
- **Então** o build deve falhar caso haja quebra de contrato ou não conformidade de nomenclatura

### Cenário 4: Teste de integração gRPC ponta a ponta
- **Dado** que o microsserviço Go e o servidor Ktor estão em execução
- **Quando** um empréstimo é criado via API REST do Ktor (`POST /api/v1/emprestimos`)
- **Então** o Ktor deve invocar o stub gRPC do Go, obter a data calculada e gravá-la no PostgreSQL
- **E** a resposta da API REST deve exibir a `data_devolucao_prevista` retornada pelo serviço Go

---

## 🔌 Especificação do Contrato gRPC

### `protos/prazos.proto`
```protobuf
syntax = "proto3";

package smorg.prazos.v1;

option go_package = "smorg/services/pkg/api/v1;prazosv1";
option java_multiple_files = true;
option java_package = "com.smorg.prazos.v1";

service PrazoService {
  rpc CalcularPrazoDevolucao (CalcularPrazoRequest) returns (CalcularPrazoResponse);
}

message CalcularPrazoRequest {
  string usuario_id = 1;
  string perfil_usuario = 2;       // Ex: "ALUNO", "MONITOR", "PROFESSOR"
  string equipamento_id = 3;
  string categoria_equipamento = 4;
}

message CalcularPrazoResponse {
  string data_devolucao_prevista = 1; // ISO-8601 UTC
  int32 dias_concedidos = 2;
  string observacoes = 3;
}
```

---

## 🛠️ Tarefas Técnicas de Implementação (Checklist)
- [ ] Configurar o plugin Protobuf/gRPC no `api/build.gradle.kts` para geração dos stubs Kotlin/Java.
- [ ] Configurar geração de stubs Go no diretório `services/pkg/api/v1`.
- [ ] Implementar o servidor gRPC em Go (`services/internal/service/prazo_service.go`) com a lógica de contagem de dias úteis.
- [ ] Adicionar testes unitários no Go cobrindo cálculo com fins de semana e feriados.
- [ ] Configurar validação de arquitetura Go com `arch-go.yml` garantindo 100% de compliance no CI.
- [ ] Implementar cliente gRPC no Ktor e injetar no caso de uso de Empréstimo (`RegistrarRetiradaUseCase`).
- [ ] Criar teste de integração gRPC ponta a ponta rodando em pipeline CI (`mise run test`).
- [ ] Configurar o `docker-compose.yml` para orquestrar Ktor, serviço Go e PostgreSQL.

---

## 🏁 Definição de Pronto (Definition of Done - DoD)
- [ ] Microsserviço Go implementado com a responsabilidade de motor de regras gRPC.
- [ ] `arch-go.yml` com compliance 100% no CI.
- [ ] `buf lint` e `buf breaking` ativos e verdes no CI.
- [ ] Stubs Go e Kotlin gerados e com checagem de sincronia.
- [ ] Teste de integração gRPC ponta a ponta (Ktor -> Go) verde no CI.
- [ ] `docker compose up` sobe os serviços e o banco do zero de forma saudável.
