package com.smorg.adapters.database

import com.smorg.domain.Emprestimo
import com.smorg.domain.EmprestimoNaoEncontrado
import com.smorg.domain.RepositorioDeEmprestimos
import com.smorg.domain.StatusEmprestimo
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class RepositorioEmprestimosPostgres(private val db: Database? = null) : RepositorioDeEmprestimos {

    private suspend fun <T> query(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, db) { block() }

    override suspend fun listar(
        status: StatusEmprestimo?,
        usuarioId: UUID?,
        equipamentoId: UUID?,
        limit: Int,
        offset: Long
    ): List<Emprestimo> = query {
        var op: Op<Boolean>? = null

        if (status != null) {
            val statusOp = (EmprestimosTable.status eq status.name)
            op = if (op == null) statusOp else (op and statusOp)
        }

        if (usuarioId != null) {
            val userOp = (EmprestimosTable.usuarioId eq usuarioId)
            op = if (op == null) userOp else (op and userOp)
        }

        if (equipamentoId != null) {
            val eqOp = (EmprestimosTable.equipamentoId eq equipamentoId)
            op = if (op == null) eqOp else (op and eqOp)
        }

        val query = if (op != null) EmprestimosTable.selectAll().where(op) else EmprestimosTable.selectAll()
        query.limit(limit, offset)
            .orderBy(EmprestimosTable.dataRetirada to SortOrder.DESC)
            .map { it.paraEmprestimo() }
    }

    override suspend fun buscarPorId(id: UUID): Emprestimo? = query {
        EmprestimosTable.selectAll()
            .where { EmprestimosTable.id eq id }
            .singleOrNull()
            ?.paraEmprestimo()
    }

    override suspend fun buscarAtivoPorEquipamento(equipamentoId: UUID): Emprestimo? = query {
        EmprestimosTable.selectAll()
            .where {
                (EmprestimosTable.equipamentoId eq equipamentoId) and
                        (EmprestimosTable.status eq StatusEmprestimo.ATIVO.name)
            }
            .singleOrNull()
            ?.paraEmprestimo()
    }

    override suspend fun criar(
        usuarioId: UUID,
        equipamentoId: UUID,
        dataRetirada: String,
        dataDevolucaoPrevista: String
    ): Emprestimo = query {
        val novoId = UUID.randomUUID()
        val statusInicial = StatusEmprestimo.ATIVO

        EmprestimosTable.insert {
            it[id] = novoId
            it[EmprestimosTable.usuarioId] = usuarioId
            it[EmprestimosTable.equipamentoId] = equipamentoId
            it[EmprestimosTable.dataRetirada] = dataRetirada
            it[EmprestimosTable.dataDevolucaoPrevista] = dataDevolucaoPrevista
            it[EmprestimosTable.dataDevolucaoReal] = null
            it[status] = statusInicial.name
            it[devolvidoComAtraso] = false
        }

        Emprestimo(
            id = novoId,
            usuarioId = usuarioId,
            equipamentoId = equipamentoId,
            dataRetirada = dataRetirada,
            dataDevolucaoPrevista = dataDevolucaoPrevista,
            dataDevolucaoReal = null,
            status = statusInicial,
            devolvidoComAtraso = false
        )
    }

    override suspend fun registrarDevolucao(
        emprestimoId: UUID,
        dataDevolucaoReal: String,
        devolvidoComAtraso: Boolean
    ): Emprestimo = query {
        val existente = EmprestimosTable.selectAll()
            .where { EmprestimosTable.id eq emprestimoId }
            .singleOrNull()
            ?: throw EmprestimoNaoEncontrado(emprestimoId.toString())

        EmprestimosTable.update({ EmprestimosTable.id eq emprestimoId }) {
            it[EmprestimosTable.dataDevolucaoReal] = dataDevolucaoReal
            it[status] = StatusEmprestimo.DEVOLVIDO.name
            it[EmprestimosTable.devolvidoComAtraso] = devolvidoComAtraso
        }

        EmprestimosTable.selectAll()
            .where { EmprestimosTable.id eq emprestimoId }
            .single()
            .paraEmprestimo()
    }

    private fun ResultRow.paraEmprestimo() = Emprestimo(
        id = this[EmprestimosTable.id],
        usuarioId = this[EmprestimosTable.usuarioId],
        equipamentoId = this[EmprestimosTable.equipamentoId],
        dataRetirada = this[EmprestimosTable.dataRetirada],
        dataDevolucaoPrevista = this[EmprestimosTable.dataDevolucaoPrevista],
        dataDevolucaoReal = this[EmprestimosTable.dataDevolucaoReal],
        status = StatusEmprestimo.valueOf(this[EmprestimosTable.status]),
        devolvidoComAtraso = this[EmprestimosTable.devolvidoComAtraso]
    )
}
