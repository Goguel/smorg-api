# ADR 001: Responsabilidade e Isolamento do Microsserviço em Go

- **Status:** Aprovada
- **Data:** 31/10/2026
- **Autores:** Miguel Xavier de Morais

---

## 1. Contexto

O sistema **SmOrg** gerencia ativos físicos em laboratórios universitários, eliminando atrito através de aproximação por NFC. Durante a operação de empréstimo de um equipamento, o sistema precisa determinar com exatidão a data limite de devolução.

Essa definição não é uma simples adição de dias fixos: ela depende do **perfil acadêmico do usuário** (Alunos recebem 3 dias úteis, Monitores recebem 7 dias úteis e Professores recebem 15 dias úteis), da categoria do equipamento e do **calendário institucional da UFRN** (que desconsidera fins de semana, feriados municipais/nacionais e dias de suspensão de atividades acadêmicas). Além disso, regras de calendário acadêmico são dinâmicas e sofrem frequentes alterações no ambiente universitário (como ajustes pós-greve ou alterações de recessos).

Na arquitetura da disciplina DIM0547 (Desenvolvimento de Sistemas Web II), exige-se a construção de um segundo serviço em Go conectado à API principal via gRPC, com responsabilidade única e justificada no domínio do produto.

---

## 2. Decisão

Decidi criar um microsserviço isolado em **Go** denominado **Motor de Regras e Prazos** (`smorg-prazos`), que se comunica com a API principal em Kotlin/Ktor através de **gRPC** com contratos em **Protocol Buffers** (pacote `smorg.prazos.v1`).

O microsserviço possui domínio próprio em Go puro (sem frameworks e sem dependência de banco de dados), respeita rigorosamente prazos via `context.Context` e realiza o cálculo de dias úteis com base no calendário acadêmico.

---

## 3. Alternativas Consideradas

### Alternativa 1: Manter a lógica dentro do serviço principal (Kotlin/Ktor)
* **Descrição:** Executar o cálculo de calendário diretamente no monólito Ktor.
* **Motivo do descarte:** Viola a separação de responsabilidades. O calendário acadêmico e as tabelas de prazos por perfil mudam com frequência. Manter essa lógica acoplada forçaria re-deploys frequentes da API central, interrompendo o serviço de autenticação NFC dos alunos. Além disso, não atenderia ao propósito pedagógico e arquitetural da disciplina de compor um sistema poliglota.

### Alternativa 2: Expor o serviço Go via REST/HTTP com JSON
* **Descrição:** Construir uma API REST tradicional em Go para responder requisições HTTP da API Ktor.
* **Motivo do descarte:** gRPC sobre HTTP/2 com Protocol Buffers oferece multiplexação na mesma conexão TCP, serialização binária ultrarrápida e contrato fortemente tipado com geração de stubs (`buf lint` / `buf generate`). Para uma chamada síncrona que bloqueia o fechamento da transação de empréstimo, a latência do gRPC é significativamente menor.

### Alternativa 3: Microsserviço Go com banco de dados próprio ou compartilhado
* **Descrição:** Permitir que o serviço Go acessasse as mesmas tabelas do PostgreSQL da API Ktor.
* **Motivo do descarte:** Compartilhar banco de dados entre microsserviços viola o princípio de desacoplamento, criando concorrência de schema e dependência oculta de infraestrutura. O serviço de prazos foi projetado para ser estritamente *stateless*, recebendo todos os parâmetros necessários via requisição gRPC.

---

## 4. Por que o serviço fica fora da API e o que aconteceria se ficasse dentro?

### Por que fica fora:
1. **Isolamento de ciclo de vida:** Permite atualizar feriados acadêmicos, prazos por categoria ou regras excepcionais do semestre sem tocar no serviço principal de persistência e sem risco de indisponibilidade da API REST.
2. **Eficiência de computação e concorrência:** Go oferece inicialização quase instantânea, uso mínimo de memória e goroutines eficientes para cálculos de lote (como varredura noturna de ativos prestes a vencer).
3. **Escala independente:** Caso a frequência de consultas de prazos e simulações aumente, o serviço Go pode escalar horizontalmente sem sobrecarregar a JVM do Ktor.

### O que aconteceria se ficasse dentro:
* Cada alteração no calendário acadêmico exigiria a recompilação e novo deploy da aplicação Ktor inteira.
* O domínio da API Ktor ficaria poluído com tabelas de feriados, recessos e regras de cálculo temporal.
* Erros ou lentidão no cômputo temporal poderiam degradar threads da JVM e prejudicar requisições simples de leitura de NFC no balcão do laboratório.

---

## 5. Consequências

### Positivas:
* **Alta coesão:** O serviço Go tem responsabilidade única e clara (motor determinístico de prazos).
* **Contrato versionado e tipado:** A comunicação gRPC elimina discrepâncias de tipagem e garante documentação estrita via Protocol Buffers.
* **Domínio Go puro:** A lógica central de cálculo é livre de dependências de rede e de banco, facilitando testes de unidade rápidos com `go test`.

### Negativas e Mitigações:
* **Dependência de chamada de rede síncrona:** A API Ktor precisa lidar com a indisponibilidade ou lentidão do serviço Go.
  * *Mitigação:* A API Ktor implementa timeout obrigatório (`withDeadlineAfter`) e política de resiliência/degradação graciosa: se o serviço Go estiver inacessível, a API Ktor adota um prazo padrão de contingência ou retorna erro amigável RFC 9457 sem quebrar o servidor.
* **Complexidade no ambiente local:** Requer execução simultânea de múltiplos processos.
  * *Mitigação:* Orquestração centralizada via `docker-compose.yml` e scripts `mise`.

