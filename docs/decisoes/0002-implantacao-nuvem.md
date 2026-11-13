# ADR 002: Estratégia de Implantação, Banco Gerenciado e Degradação Graciosa

- **Status:** Aprovada
- **Data:** 31/10/2026
- **Autores:** Miguel Xavier de Morais

---

## 1. Contexto

O **SmOrg** necessita de um ambiente em nuvem para que a API REST possa ser consumida externamente pelo aplicativo mobile ([smorg-mobile](https://github.com/Goguel/smorg-mobile)) e avaliada pela banca da disciplina DIM0547.

Os requisitos da disciplina estipulam:
1. Banco de dados relacional gerenciado (Neon Serverless PostgreSQL).
2. API executando em plataforma de container (Render ou similar) com health check configurado.
3. Migrações de banco aplicadas de forma automatizada.
4. Nenhuma credencial ou segredo versionado no repositório.
5. Definição clara sobre a implantação do microsserviço Go e comportamento da API caso ele não esteja acessível.

---

## 2. Decisão

1. **Banco Gerenciado:** Adotei o **Neon PostgreSQL**, aproveitando a camada serverless gratuita e os pools de conexão integrados.
2. **Plataforma de Container:** Adotei o **Render** para hospedar o container Docker da API principal Ktor (`smorg-api`), acessível publicamente em `https://smorg-api.onrender.com` com Swagger UI em `/docs`.
3. **Gestão de Segredos:** Todas as informações sensíveis (`STORAGE_JDBCURL`, `STORAGE_USER`, `STORAGE_PASSWORD`) são gerenciadas exclusivamente através das *Environment Variables* da plataforma de hospedagem, sem constar em nenhum arquivo versionado no Git.
4. **Execução de Migrações:** O Flyway executa as migrações versionadas (`V1`, `V2`) automaticamente durante o processo de inicialização do container da API, garantindo que o esquema esteja sincronizado antes da abertura da porta HTTP.
5. **Decisão sobre o Microsserviço Go em Nuvem:**
   - Conforme nota oficial da disciplina em `RUBRICAS.md §Sprint 2`, as plataformas gratuitas de containers não oferecem suporte à comunicação gRPC interna/privada entre instâncias sem custo (no Render, serviços privados são pagos e web services gratuitos não recebem tráfego HTTP/2 gRPC puro).
   - Portanto, em nuvem, mantemos a **API Ktor** e o **PostgreSQL (Neon)** em execução. A API Ktor foi desenhada para ser resiliente: na ausência do serviço Go, a camada adaptadora gRPC ativa a **política de fallback / degradação graciosa**, concedendo prazos padrão de contingência sem interromper requisições ou causar erros 500 para o aplicativo cliente.
   - O microsserviço Go e a integração gRPC síncrona completa são avaliados e demonstrados localmente através do comando unificado `docker compose up --build` e nos testes automatizados do CI (`.github/workflows/ci.yml`).

---

## 3. Alternativas Consideradas

### Alternativa 1: Contratar planos pagos ou VPS dedicada
* **Motivo do descarte:** A disciplina restringe os custos de infraestrutura a serviços gratuitos para viabilidade acadêmica de todos os estudantes.

### Alternativa 2: Aplicar migrações Flyway manualmente pela máquina do desenvolvedor
* **Motivo do descarte:** Processo propenso a esquecimentos e erros manuais. A execução no startup da API ou em pipeline de CI assegura idempotência e consistência do banco de dados em qualquer ambiente.

### Alternativa 3: Expor o serviço Go em porta HTTP pública aberta na nuvem
* **Motivo do descarte:** A comunicação interna gRPC entre microsserviços não deve trafegar desprotegida pela internet pública sem autenticação mTLS e firewall, além de acrescentar latência desnecessária.

---

## 4. Consequências

### Positivas:
* Banco robusto, gerenciado na nuvem com backups automáticos e alta disponibilidade.
* Total conformidade com as boas práticas de segurança (ausência total de senhas no repositório).
* Resiliência da API: o usuário do aplicativo móvel nunca fica bloqueado caso um serviço secundário de cálculo fique fora do ar.
* Ambiente local idêntico ao de produção via `docker-compose.yml`.

### Negativas e Mitigações:
* No ambiente gratuito em nuvem, os prazos concedidos operam no modo de contingência local.
  * *Mitigação:* A integração gRPC é 100% testada localmente via Testcontainers e servidores em memória, bem como demonstrada na íntegra no vídeo da sprint e na daily meeting.

