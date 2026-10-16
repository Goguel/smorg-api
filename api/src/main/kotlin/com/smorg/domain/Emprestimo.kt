package com.smorg.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class StatusEmprestimo {
    ATIVO,
    DEVOLVIDO
}

@Serializable
data class Emprestimo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("usuario_id")
    val usuarioId: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("equipamento_id")
    val equipamentoId: UUID,
    @SerialName("data_retirada")
    val dataRetirada: String,
    @SerialName("data_devolucao_prevista")
    val dataDevolucaoPrevista: String,
    @SerialName("data_devolucao_real")
    val dataDevolucaoReal: String? = null,
    val status: StatusEmprestimo,
    @SerialName("devolvido_com_atraso")
    val devolvidoComAtraso: Boolean = false,
    val equipamento: Equipamento? = null
)

@Serializable
data class NovoEmprestimo(
    @Serializable(with = UUIDSerializer::class)
    @SerialName("usuario_id")
    val usuarioId: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("equipamento_id")
    val equipamentoId: UUID? = null,
    @SerialName("nfc_tag_id")
    val nfcTagId: String? = null
) {
    fun violacoes(): List<String> = buildList {
        if (equipamentoId == null && (nfcTagId == null || nfcTagId.isBlank())) {
            add("equipamento: informe 'equipamento_id' ou 'nfc_tag_id'")
        }
    }
}

@Serializable
data class RegistrarDevolucao(
    @Serializable(with = UUIDSerializer::class)
    @SerialName("emprestimo_id")
    val emprestimoId: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("equipamento_id")
    val equipamentoId: UUID? = null,
    @SerialName("nfc_tag_id")
    val nfcTagId: String? = null
) {
    fun violacoes(): List<String> = buildList {
        if (emprestimoId == null && equipamentoId == null && (nfcTagId == null || nfcTagId.isBlank())) {
            add("identificacao: informe 'emprestimo_id', 'equipamento_id' ou 'nfc_tag_id'")
        }
    }
}

// Exceções do Domínio de Empréstimos
class EmprestimoNaoEncontrado(val id: String) : RuntimeException("Empréstimo '$id' não encontrado.")

class EquipamentoIndisponivel(val tagOuId: String, val statusAtual: StatusEquipamento) :
    RuntimeException("Equipamento '$tagOuId' não está disponível para empréstimo (status atual: $statusAtual).")

class ItemNaoEmprestado(val tagOuId: String) :
    RuntimeException("O equipamento '$tagOuId' não possui empréstimo ativo para devolução.")

// Porta do Repositório de Empréstimos
interface RepositorioDeEmprestimos {
    suspend fun listar(
        status: StatusEmprestimo? = null,
        usuarioId: UUID? = null,
        equipamentoId: UUID? = null,
        limit: Int = 20,
        offset: Long = 0
    ): List<Emprestimo>

    suspend fun buscarPorId(id: UUID): Emprestimo?
    suspend fun buscarAtivoPorEquipamento(equipamentoId: UUID): Emprestimo?
    suspend fun criar(
        usuarioId: UUID,
        equipamentoId: UUID,
        dataRetirada: String,
        dataDevolucaoPrevista: String
    ): Emprestimo

    suspend fun registrarDevolucao(
        emprestimoId: UUID,
        dataDevolucaoReal: String,
        devolvidoComAtraso: Boolean
    ): Emprestimo
}
