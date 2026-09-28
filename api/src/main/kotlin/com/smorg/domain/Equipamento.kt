package com.smorg.domain

import java.util.UUID

data class Equipamento(
    val id: UUID,
    val nome: String,
    val nfcTagId: String?,
    val status: StatusEquipamento,
    val categoria: String
)

enum class StatusEquipamento {
    DISPONIVEL, EMPRESTADO, MANUTENCAO, INATIVO
}
