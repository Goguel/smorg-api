# Proposta de Projeto: SmOrg (Smart Organization) - API

## 1. Visão do Produto
**Para** alunos, monitores e professores responsáveis por laboratórios universitários  
**Que** enfrentam lentidão, controles manuais ineficientes e atrito na retirada e devolução de equipamentos físicos (como multímetros e kits)  
**O SmOrg é um** sistema de gestão de ativos   
**Que** automatiza a identificação, a reserva e o controle de devolução de materiais de forma instantânea  
**Diferente de** planilhas em papel, anotações informais ou sistemas lentos baseados em digitação de códigos de patrimônio  
**Nosso produto** utiliza tags NFC nos equipamentos para proporcionar uma experiência de "atrito zero", validando empréstimos e devoluções apenas aproximando o celular, tudo suportado por um backend poliglota com auditoria via gRPC.

## 2. Definição do MVP

**Hipótese de Valor:** Acreditamos que alunos e monitores vão registrar empréstimos com 100% de precisão porque a leitura física do NFC elimina a frustração da digitação de códigos e agiliza o processo no dia a dia.

| No MVP | Fora do MVP |
|---|---|
| CRUD de equipamentos e perfis de usuários | Notificações push ou e-mail de aviso de atraso |
| API para registro de empréstimos e devoluções via leitura de NFC | Fila de espera virtual para itens ocupados |
| Integração gRPC com microsserviço Go para cálculo de prazos | Painel administrativo web (Dashboard) |
| Lógica transacional de bloqueio de usuários com atrasos pendentes | Histórico analítico e geração de relatórios PDF |

## 3. Backlog Inicial
As histórias detalhadas e o quadro Kanban estão disponíveis no [GitHub Project SmOrg](https://github.com/users/Goguel/projects/2) e documentadas na pasta [`docs/backlog/`](./backlog/README.md).

| Prio | História | Estimativa | Critérios de aceitação | Sprint | Documento Detalhado |
|---|---|---|---|---|---|
| P1 | Como monitor, quero cadastrar novos equipamentos com seus IDs de tag NFC para incluí-los no acervo do laboratório | 5 pts | CRUD com ID único validado | 1 | [Issue #1](./backlog/issue-1-crud-equipamentos.md) |
| P1 | Como aluno, quero registrar a retirada de um equipamento para ter a permissão formal de uso | 5 pts | Status do item muda para indisponível; gera transação | 1 | [Issue #2](./backlog/issue-2-registrar-retirada.md) |
| P1 | Como sistema Ktor, quero comunicar com o serviço Go no momento do empréstimo para calcular o prazo exato | 8 pts | gRPC implementado; calcula data ignorando fins de semana | 2 | [Issue #3](./backlog/issue-3-comunicar-com-go.md) |
| P1 | Como aluno, quero registrar a devolução de um equipamento para liberar o item para outros colegas | 3 pts | Finaliza a transação; status do item muda para disponível | 1 | [Issue #4](./backlog/issue-4-registrar-devolucao.md) |
| P2 | Como administrador, quero consultar o inventário de itens emprestados para auditoria | 3 pts | Endpoint retorna itens filtrados por status | 1 | [Issue #5](./backlog/issue-5-consultar-inventario.md) |
| P3 | Como sistema Ktor, quero salvar os itens mais requisitados em cache para reduzir latência | 5 pts | Cache ativado com métricas de hit/miss | 3 | [Issue #6](./backlog/issue-6-cache-itens-requisitados.md) |

## 4. Entidades Principais do Domínio
* **Usuario:** Representa alunos, monitores e administradores (id, matricula, nome, perfil).
* **Equipamento:** Representa o ativo físico do laboratório (id, nfc_tag_id, nome, descricao, status).
* **Emprestimo:** Representa a transação histórica (id, usuario_id, equipamento_id, data_retirada, data_devolucao_prevista, data_devolucao_real).

## 5. Decisão de Stack: Kotlin com Ktor
Optamos por **Kotlin com Ktor** pois a equipe desenvolverá simultaneamente o aplicativo móvel utilizando Kotlin Multiplatform (KMP). Essa escolha permite um reaproveitamento expressivo de conhecimento sintático e facilita a padronização de contratos (DTOs) entre o frontend e a API. Além disso, as corrotinas nativas do Kotlin permitirão tratar o tráfego concorrente de chamadas de rede da API de forma muito eficiente, sem a complexidade de alocação de threads tradicional do Java.

## 6. Divisão de Responsabilidades: Ktor vs. Go
* **Serviço Principal (Ktor):** Responsável por orquestrar os casos de uso principais (CRUD de equipamentos, registro de usuários, abertura e fechamento de empréstimos) e gerenciar a persistência no banco de dados relacional (PostgreSQL).
* **Microsserviço (Go):** Atuará como um "Motor de Regras e Prazos". A API Ktor fará chamadas a ele via gRPC delegando os cálculos rigorosos. O microsserviço Go cruzará o perfil do usuário, o tipo de equipamento e o calendário acadêmico para devolver rapidamente a data limite exata de devolução, justificando sua existência pelo processamento isolado de regras de negócio mutáveis e intensivas.

## 7. Equipe
* Miguel Xavier de Morais - Matrícula: 20240027427 - Papel: Backend (Kotlin/Go)  
Obs: Sou o único nesse projeto mas escrevi tudo no plural para padronização

## 8. Coorte e Integrações
* **Coorte escolhida para apresentação:** Coorte A — Presencial  
* **Integração Declarada:** Este projeto está formalmente integrado com a disciplina de Desenvolvimento para Dispositivos Móveis (DIM0524). O backend desenvolvido aqui consumirá as requisições geradas pelo aplicativo móvel presente no repositório: https://github.com/Goguel/smorg-mobile.