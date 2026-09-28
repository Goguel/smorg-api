package com.smorg.adapters.database

import org.jetbrains.exposed.sql.Table

object EquipamentosTable : Table("equipamentos") {
    val id = uuid("id")
    val nome = varchar("nome", 255)
    val nfcTagId = varchar("nfc_tag_id", 50).nullable()
    val status = varchar("status", 50)
    val categoria = varchar("categoria", 50)

    override val primaryKey = PrimaryKey(id)
}
