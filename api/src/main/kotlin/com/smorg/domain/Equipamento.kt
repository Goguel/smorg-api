package com.smorg.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
enum class StatusEquipamento {
    DISPONIVEL,
    EMPRESTADO,
    MANUTENCAO,
    INATIVO
}

@Serializable
data class Equipamento(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val nome: String,
    @SerialName("nfc_tag_id")
    val nfcTagId: String? = null,
    val status: StatusEquipamento,
    val categoria: String
)

@Serializable
data class NovoEquipamento(
    val nome: String,
    @SerialName("nfc_tag_id")
    val nfcTagId: String? = null,
    val categoria: String
) {
    fun violacoes(): List<String> = buildList {
        if (nome.isBlank()) add("nome: não pode ficar em branco")
        if (categoria.isBlank()) add("categoria: não pode ficar em branco")
        if (nfcTagId != null && nfcTagId.isBlank()) add("nfc_tag_id: se fornecido, não pode ficar em branco")
    }
}

@Serializable
data class AtualizarEquipamento(
    val nome: String? = null,
    @SerialName("nfc_tag_id")
    val nfcTagId: String? = null,
    val status: StatusEquipamento? = null,
    val categoria: String? = null
) {
    fun violacoes(): List<String> = buildList {
        if (nome != null && nome.isBlank()) add("nome: não pode ficar em branco")
        if (categoria != null && categoria.isBlank()) add("categoria: não pode ficar em branco")
        if (nfcTagId != null && nfcTagId.isBlank()) add("nfc_tag_id: se fornecido, não pode ficar em branco")
    }
}

// Exceções de Domínio
class EntradaInvalida(val violacoes: List<String>) : RuntimeException(violacoes.joinToString("; "))

class ConflitoTagNfc(val tag: String) : RuntimeException("Já existe um equipamento cadastrado com a tag NFC '$tag'.")

class EquipamentoNaoEncontrado(val identificador: String) : RuntimeException("Equipamento '$identificador' não encontrado.")

class OperacaoNaoPermitida(override val message: String) : RuntimeException(message)

// Porta do Repositório
interface RepositorioDeEquipamentos {
    suspend fun listar(status: StatusEquipamento? = null, categoria: String? = null): List<Equipamento>
    suspend fun buscarPorId(id: UUID): Equipamento?
    suspend fun buscarPorTag(nfcTagId: String): Equipamento?
    suspend fun adicionar(novo: NovoEquipamento): Equipamento
    suspend fun atualizar(id: UUID, atualizacao: AtualizarEquipamento): Equipamento
    suspend fun excluir(id: UUID)
}
