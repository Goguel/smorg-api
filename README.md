# SmOrg (Smart Organization) - API

Repositório do backend do sistema **SmOrg**, um gerenciador de ativos físicos por aproximação (NFC) focado em atrito zero para laboratórios universitários. Desenvolvido para a disciplina DIM0547 — Desenvolvimento de Sistemas Web II.

O documento detalhado da visão do produto, MVP e decisões arquiteturais encontra-se em [`docs/proposta.md`](docs/proposta.md).

## Equipe e Informações Acadêmicas
* **Aluno:** Miguel Xavier de Morais (Matrícula: 20240027427)
* **Coorte de Apresentação:** Presencial
* **Integração:** Este projeto de backend está integrado com a disciplina DIM0524 (Sistemas Móveis). O App que consome esta API está no repositório [smorg-mobile](https://github.com/Goguel/smorg-mobile).

## Arquitetura
O backend é um monorepo poliglota construído com:
* **Serviço Principal:** Kotlin com Ktor (Responsável por orquestração, regras de negócio gerais e banco de dados).
* **Microsserviço:** Go (Responsável por cálculos precisos de prazos, atuando como um Motor de Regras isolado).
* **Comunicação interna:** gRPC + Protocol Buffers (Buf).
* **Banco de Dados:** PostgreSQL (gerenciado no Neon).

## Como Rodar Localmente

O projeto utiliza o gerenciador de tarefas `mise` em conjunto com o Docker Compose para garantir um ambiente reproduzível.

### Pré-requisitos
1. [Docker Desktop](https://www.docker.com/products/docker-desktop/) rodando.
2. [`mise`](https://mise.jdx.dev/getting-started.html) instalado.

### Comandos Principais
Na raiz do projeto, execute os seguintes comandos:

* `mise run build`: Instala dependências e compila todos os serviços (Ktor e Go).
* `mise run test`: Executa a suíte de testes unitários e de integração (usa Testcontainers).
* `mise run up`: Sobe o ambiente completo (API, Serviço Go e Banco de Dados) utilizando o `docker-compose.yml`.
* `mise run ci`: Reproduz localmente o exato pipeline de integração contínua do GitHub Actions.

---

## Checklist de Avaliação (Rúbricas)

### Sprint 0
- [x] Monorepo público: api/ services/ protos/ docs/ mise.toml docker-compose.yml
- [x] mise run build && mise run test passam localmente
- [x] CI verde (build dos dois stacks)
- [x] docs/proposta.md com justificativa Ktor×Quarkus e serviço principal×Go
- [x] Coorte (A/B) e integração declaradas
- [x] Vídeo 5 min

### Sprint 1
- [ ] CRUD de ≥2 entidades com relacionamento
- [ ] Teste de arquitetura da regra de dependência rodando no CI
- [ ] Migrações Flyway (sem geração automática de schema)
- [ ] Testes com Testcontainers verdes local E no CI
- [ ] Validação + problem details + OpenAPI
- [ ] Vídeo 5 min

### Sprint 2
- [ ] Microsserviço Go com responsabilidade justificada
- [ ] arch-go.yml com compliance 100 no CI
- [ ] protos/ com buf lint + buf breaking no CI
- [ ] Stubs Go e (Kotlin/Java) gerados (check de sincronia)
- [ ] Teste de integração gRPC ponta a ponta
- [ ] docker compose up sobe tudo do zero
- [ ] Vídeo 5 min

### Sprint 3
- [ ] Banco no Neon com migrações via CI/task
- [ ] Cache com política declarada + métricas hit/miss
- [ ] Deploy no ar (Northflank/Render/Railway) com URL no README
- [ ] Imagens publicadas no ghcr.io pelo CI
- [ ] Logs estruturados + health checks
- [ ] Suíte verde local E remoto
- [ ] Vídeo 5 min

### Entrega Final
- [ ] Sistema no ar, deploy automatizado de main
- [ ] Auth (JWT+refresh ou OIDC) + teste anti-BOLA
- [ ] docs/seguranca.md cobrindo OWASP API Top 10
- [ ] renovate.json ativo, Semgrep no CI, zero segredos versionados
- [ ] Pipeline completo verde
- [ ] Site de documentação público (≥3 ADRs)
- [ ] Referência de API pública gerada do OpenAPI
- [ ] README permite rodar em <10 min + licença
- [ ] Vídeo 10 min
- [ ] Apresentação ao vivo