# Issue #6: Armazenamento em Cache de Itens Requisitados e Métricas de Observabilidade

- **Título Sugerido:** `[US06] Armazenamento em cache de equipamentos frequentes e métricas hit/miss`
- **Tipo:** `type:user-story`
- **Prioridade:** `prio:P3`
- **Estimativa:** `5 pts`
- **Sprint:** `sprint:sprint-3`
- **Componentes:** `component:api-ktor`, `component:cache`

---

## 👤 Descrição da História
- **Como** sistema Ktor (API Principal)
- **Quero** armazenar em cache os dados dos equipamentos mais consultados (especialmente nas buscas por Tag NFC) com política declarada de expiração e invalidação
- **Para que** a latência percebida no aplicativo móvel seja minimizada, o banco de dados Neon seja poupado de consultas repetitivas e métricas de acerto/erro (hit/miss) sejam coletadas

---

## 📋 Contexto e Regras de Negócio

1. **Latência Crítica no NFC:** Ao aproximar o celular da tag NFC, o aplicativo consulta imediatamente `GET /api/v1/equipamentos/tag/{nfcTagId}`. Como essa rota é acionada a cada interação física, ela deve responder com a menor latência possível (<20ms).
2. **Política Declarada de Cache (Rúbrica Sprint 3):**
   - **Tecnologia:** Cache em memória de alta performance (Caffeine) ou Redis gerenciado.
   - **Estratégia:** *Cache-Aside* com chave composta `equipamento:tag:{nfc_tag_id}` e `equipamento:id:{id}`.
   - **TTL (Time to Live):** 15 minutos para consultas de equipamentos disponíveis.
   - **Evicção:** Baseada em LRU (*Least Recently Used*) com capacidade máxima de 1.000 entradas.
3. **Invalidação Reativa Obrigatória:** O cache não pode servir dados defasados (*stale data*). Quando ocorrer:
   - Registro de retirada/empréstimo (`POST /api/v1/emprestimos`)
   - Registro de devolução (`POST /api/v1/emprestimos/devolucao`)
   - Atualização ou inativação do equipamento (`PUT/DELETE /api/v1/equipamentos/{id}`)
   As chaves correspondentes no cache devem ser **imediatamente invalidadas**.
4. **Métricas de Hit e Miss:** A API deve instrumentar e expor contadores de `cache_hits_total` e `cache_misses_total` no endpoint de observabilidade `/metrics` (formato Prometheus via Micrometer).

---

## 🎯 Critérios de Aceitação (BDD / Given-When-Then)

### Cenário 1: Primeira consulta ao equipamento gera Cache Miss
- **Dado** que o cache acabou de ser iniciado ou o item nunca foi consultado
- **Quando** for realizada a requisição `GET /api/v1/equipamentos/tag/{nfcTagId}`
- **Então** a API deve consultar o banco de dados PostgreSQL
- **E** armazenar o resultado no cache
- **E** o contador de `cache_misses_total` deve ser incrementado em 1

### Cenário 2: Consulta subsequente gera Cache Hit de baixa latência
- **Dado** que o item com tag `04A1B2C3D4` já está armazenado no cache
- **Quando** uma nova requisição `GET /api/v1/equipamentos/tag/04A1B2C3D4` for realizada dentro da janela de TTL
- **Então** os dados devem ser retornados diretamente da memória sem executar query SQL no banco
- **E** o contador de `cache_hits_total` deve ser incrementado em 1
- **E** o tempo de resposta deve ser inferior a 25ms

### Cenário 3: Invalidação imediata do cache na mudança de status
- **Dado** que o equipamento `04A1B2C3D4` está no cache como `DISPONIVEL`
- **Quando** um aluno registrar o empréstimo deste equipamento
- **Então** a entrada no cache deve ser removida
- **E** a próxima requisição de consulta àquela tag deve refletir o status atualizado `EMPRESTADO` diretamente do banco

### Cenário 4: Exposição pública de métricas de cache
- **Dado** que o sistema esteve em operação processando leituras
- **Quando** o monitoramento requisitar `GET /metrics`
- **Então** a resposta deve conter as métricas `cache_hits_total` e `cache_misses_total` com valores numéricos consistentes

---

## 🔌 Especificação de Métricas e Observabilidade

### `GET /metrics` (Prometheus)
```promql
# HELP cache_hits_total Total de leituras atendidas pelo cache
# TYPE cache_hits_total counter
cache_hits_total{cache="equipamentos"} 142

# HELP cache_misses_total Total de consultas que necessitaram de acesso ao banco
# TYPE cache_misses_total counter
cache_misses_total{cache="equipamentos"} 18

# Taxa calculada de Hit Ratio: 142 / (142 + 18) = 88.75%
```

---

## 🛠️ Tarefas Técnicas de Implementação (Checklist)
- [ ] Adicionar dependência do Caffeine Cache e Micrometer Prometheus no `api/build.gradle.kts`.
- [ ] Criar classe gerenciadora de cache `EquipmentCacheManager` com métricas integradas.
- [ ] Aplicar interceptação de cache no caso de uso `ConsultarEquipamentoUseCase`.
- [ ] Implementar evento ou chamada de invalidação de chave em `RegistrarRetiradaUseCase` e `RegistrarDevolucaoUseCase`.
- [ ] Configurar endpoint `/metrics` no Ktor para expor os contadores.
- [ ] Escrever testes unitários e de integração validando hit, miss e invalidação sob transações.

---

## 🏁 Definição de Pronto (Definition of Done - DoD)
- [ ] Política de cache formalmente documentada (TTL, evicção e invalidação).
- [ ] Invalidação imediata funcionando perfeitamente em empréstimos e devoluções.
- [ ] Endpoint `/metrics` expondo métricas de hit/miss no padrão Prometheus.
- [ ] Suíte de testes automatizados comprovando a taxa de hit/miss e invalidação passando no CI.
