package com.smorg.adapters.database

import org.jetbrains.exposed.sql.Table

object EmprestimosTable : Table("emprestimos") {
    val id = uuid("id")
    val usuarioId = uuid("usuario_id")
    val equipamentoId = uuid("equipamento_id").references(EquipamentosTable.id)
    val dataRetirada = varchar("data_retirada", 50)
    val dataDevolucaoPrevista = varchar("data_devolucao_prevista", 50)
    val dataDevolucaoReal = varchar("data_devolucao_real", 50).nullable()
    val status = varchar("status", 50)
    val devolvidoComAtraso = bool("devolvido_com_atraso")

    override val primaryKey = PrimaryKey(id)
}
