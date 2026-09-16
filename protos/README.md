# Contratos de Comunicação (Protocol Buffers)

Esta pasta contém as definições de contratos gRPC / Protocol Buffers utilizados para comunicação entre o serviço principal (Kotlin/Ktor) e os microsserviços (Go).

## Estrutura
* `prazos.proto`: Contrato de cálculo de prazos e motor de regras de devolução de materiais acadêmicos.

Na Sprint 2, este diretório será integrado ao `buf` para `buf lint` e geração automática de stubs de Go e Kotlin.
