# SmOrg (Smart Organization) - API

Repositório do backend do sistema **SmOrg**, um gerenciador de ativos físicos por aproximação (NFC) focado em atrito zero para laboratórios universitários. Desenvolvido para a disciplina DIM0547 — Desenvolvimento de Sistemas Web II.

O documento detalhado da visão do produto, MVP e decisões arquiteturais encontra-se em [`docs/proposta.md`](docs/proposta.md).

## Equipe e Informações Acadêmicas
* **Aluno:** Miguel Xavier de Morais (Matrícula: 20240027427)
* **Coorte de Apresentação:** Coorte A (Presencial)
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

## Documentação da API (OpenAPI e Swagger UI)
Quando a aplicação estiver em execução, a documentação interativa e a especificação OpenAPI estarão disponíveis em:
* **Swagger UI:** `http://localhost:8080/docs`
* **JSON Schema / OpenAPI Spec:** `http://localhost:8080/openapi.json`

---

## Checklist de Avaliação (Rúbricas Atualizadas)

### Sprint 0
- [x] Monorepo público: api/ services/ protos/ docs/ mise.toml docker-compose.yml
- [x] mise run build && mise run test passam localmente
- [x] CI verde (build dos dois stacks)
- [x] docs/proposta.md com justificativa Ktor×Quarkus e serviço principal×Go
- [x] Coorte (A/B) e integração declaradas
- [x] Vídeo 5 min

### Sprint 1
- [x] CRUD de ≥2 entidades com relacionamento (`Equipamento` e `Emprestimo`)
- [x] Camadas separadas: o domínio não importa framework nem infraestrutura
- [x] Migrações Flyway versionadas (`V1` e `V2`, sem geração automática de schema)
- [x] Validação + Problem Details (RFC 9457 / RFC 7807) + OpenAPI (Swagger UI em `/docs`)
- [ ] Vídeo 5 min: [Link do Vídeo](URL_DO_VIDEO_AQUI)

### Sprint 2 (final)
- [ ] Microsserviço Go com responsabilidade justificada
- [ ] protos/ com buf lint no CI e stubs gerados
- [ ] Teste de integração gRPC ponta a ponta
- [ ] docker compose up sobe tudo do zero
- [ ] API no ar, com URL pública no README e banco gerenciado; a API funciona mesmo sem o serviço Go
- [ ] Migrações por task ou CI; nenhum segredo no repositório
- [ ] Testes unitários, de integração (Testcontainers ou Dev Services) e de arquitetura, verdes local E no CI
- [ ] README roda em menos de 10 min; OpenAPI acessível; 2 ADRs novas
- [ ] Vídeo 5 min