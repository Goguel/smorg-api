package com.smorg.adapters.database

import com.smorg.domain.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class RepositorioEquipamentosPostgres(private val db: Database? = null) : RepositorioDeEquipamentos {

    private suspend fun <T> query(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, db) { block() }

    override suspend fun listar(status: StatusEquipamento?, categoria: String?): List<Equipamento> = query {
        var op: Op<Boolean>? = null

        if (status != null) {
            val statusOp = (EquipamentosTable.status eq status.name)
            op = if (op == null) statusOp else (op and statusOp)
        }

        if (categoria != null) {
            val catOp = (EquipamentosTable.categoria eq categoria)
            op = if (op == null) catOp else (op and catOp)
        }

        val query = if (op != null) EquipamentosTable.selectAll().where(op) else EquipamentosTable.selectAll()
        query.map { it.paraEquipamento() }
    }

    override suspend fun buscarPorId(id: UUID): Equipamento? = query {
        EquipamentosTable.selectAll()
            .where { EquipamentosTable.id eq id }
            .singleOrNull()
            ?.paraEquipamento()
    }

    override suspend fun buscarPorTag(nfcTagId: String): Equipamento? = query {
        EquipamentosTable.selectAll()
            .where { EquipamentosTable.nfcTagId eq nfcTagId }
            .singleOrNull()
            ?.paraEquipamento()
    }

    override suspend fun adicionar(novo: NovoEquipamento): Equipamento = query {
        if (!novo.nfcTagId.isNullOrBlank()) {
            val existe = EquipamentosTable.selectAll()
                .where { EquipamentosTable.nfcTagId eq novo.nfcTagId }
                .count() > 0
            if (existe) {
                throw ConflitoTagNfc(novo.nfcTagId)
            }
        }

        val novoId = UUID.randomUUID()
        val statusInicial = StatusEquipamento.DISPONIVEL

        EquipamentosTable.insert {
            it[id] = novoId
            it[nome] = novo.nome
            it[nfcTagId] = novo.nfcTagId
            it[status] = statusInicial.name
            it[categoria] = novo.categoria
        }

        Equipamento(
            id = novoId,
            nome = novo.nome,
            nfcTagId = novo.nfcTagId,
            status = statusInicial,
            categoria = novo.categoria
        )
    }

    override suspend fun atualizar(id: UUID, atualizacao: AtualizarEquipamento): Equipamento = query {
        val existente = EquipamentosTable.selectAll()
            .where { EquipamentosTable.id eq id }
            .singleOrNull()
            ?: throw EquipamentoNaoEncontrado(id.toString())

        if (!atualizacao.nfcTagId.isNullOrBlank()) {
            val conflito = EquipamentosTable.selectAll()
                .where { (EquipamentosTable.nfcTagId eq atualizacao.nfcTagId) and (EquipamentosTable.id neq id) }
                .count() > 0
            if (conflito) {
                throw ConflitoTagNfc(atualizacao.nfcTagId)
            }
        }

        EquipamentosTable.update({ EquipamentosTable.id eq id }) {
            if (atualizacao.nome != null) it[nome] = atualizacao.nome
            if (atualizacao.nfcTagId != null) it[nfcTagId] = atualizacao.nfcTagId
            if (atualizacao.status != null) it[status] = atualizacao.status.name
            if (atualizacao.categoria != null) it[categoria] = atualizacao.categoria
        }

        EquipamentosTable.selectAll()
            .where { EquipamentosTable.id eq id }
            .single()
            .paraEquipamento()
    }

    override suspend fun excluir(id: UUID): Unit = query {
        val existente = EquipamentosTable.selectAll()
            .where { EquipamentosTable.id eq id }
            .singleOrNull()
            ?: throw EquipamentoNaoEncontrado(id.toString())

        val statusAtual = StatusEquipamento.valueOf(existente[EquipamentosTable.status])
        if (statusAtual == StatusEquipamento.EMPRESTADO) {
            throw OperacaoNaoPermitida("Equipamento com status EMPRESTADO não pode ser removido ou inativado.")
        }

        EquipamentosTable.update({ EquipamentosTable.id eq id }) {
            it[status] = StatusEquipamento.INATIVO.name
        }
    }

    private fun ResultRow.paraEquipamento() = Equipamento(
        id = this[EquipamentosTable.id],
        nome = this[EquipamentosTable.nome],
        nfcTagId = this[EquipamentosTable.nfcTagId],
        status = StatusEquipamento.valueOf(this[EquipamentosTable.status]),
        categoria = this[EquipamentosTable.categoria]
    )
}
