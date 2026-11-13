# Contratos de Comunicação (Protocol Buffers)

Este diretório contém os contratos gRPC e Protocol Buffers do SmOrg, governados pelo **Buf CLI**.

## Estrutura
* `buf.yaml`: Configuração do módulo Buf com as regras de lint `STANDARD` e `COMMENTS` (conforme exigido na disciplina).
* `buf.gen.yaml`: Configuração para geração automática dos stubs Go para o microsserviço em `services/gen/`.
* `smorg/prazos/v1/prazos.proto`: Contrato oficial versionado do serviço de cálculo de prazos (`PrazoService`).

## Comandos do Buf
* Validar conformidade e documentação do contrato:
  ```bash
  cd protos
  buf lint
  ```
* Gerar os stubs Go:
  ```bash
  cd protos
  buf generate
  ```
* Verificar quebra de compatibilidade (breaking changes):
  ```bash
  cd protos
  buf breaking --against ".git#branch=main"
  ```
