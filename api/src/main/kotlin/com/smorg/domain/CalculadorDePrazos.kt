package com.smorg.domain

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Parâmetros de entrada para o cálculo do prazo de devolução de um ativo.
 */
@Serializable
data class DadosCalculoPrazo(
    @Serializable(with = UUIDSerializer::class)
    val usuarioId: UUID,
    val perfilUsuario: String = "ALUNO",
    @Serializable(with = UUIDSerializer::class)
    val equipamentoId: UUID,
    val categoriaEquipamento: String? = null,
    val dataRetirada: String? = null
)

/**
 * Resultado retornado pelo cálculo de prazos.
 */
@Serializable
data class PrazoCalculado(
    val dataDevolucaoPrevista: String,
    val diasConcedidos: Int,
    val observacoes: String
)

/**
 * Porta de saída (Clean Architecture): define a interface para o cálculo de datas
 * e regras de empréstimo. O domínio é puro e agnóstico à implementação (gRPC, memória ou HTTP).
 */
fun interface CalculadorDePrazos {
    suspend fun calcular(dados: DadosCalculoPrazo): PrazoCalculado
}

