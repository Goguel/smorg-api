package com.smorg

import com.smorg.domain.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.test.*

class EmprestimoTest {

    private class RepositorioEquipamentosFake : RepositorioDeEquipamentos {
        val itens = mutableListOf<Equipamento>()

        override suspend fun listar(
            status: StatusEquipamento?,
            categoria: String?,
            limit: Int,
            offset: Long
        ): List<Equipamento> = itens.filter {
            (status == null || it.status == status) && (categoria == null || it.categoria == categoria)
        }.drop(offset.toInt()).take(limit)

        override suspend fun buscarPorId(id: UUID): Equipamento? = itens.find { it.id == id }

        override suspend fun buscarPorTag(nfcTagId: String): Equipamento? = itens.find { it.nfcTagId == nfcTagId }

        override suspend fun adicionar(novo: NovoEquipamento): Equipamento {
            if (!novo.nfcTagId.isNullOrBlank() && itens.any { it.nfcTagId == novo.nfcTagId }) {
                throw ConflitoTagNfc(novo.nfcTagId)
            }
            val criado = Equipamento(
                id = UUID.randomUUID(),
                nome = novo.nome,
                nfcTagId = novo.nfcTagId,
                status = StatusEquipamento.DISPONIVEL,
                categoria = novo.categoria
            )
            itens.add(criado)
            return criado
        }

        override suspend fun atualizar(id: UUID, atualizacao: AtualizarEquipamento): Equipamento {
            val idx = itens.indexOfFirst { it.id == id }
            if (idx == -1) throw EquipamentoNaoEncontrado(id.toString())
            val atual = itens[idx]
            val atualizado = atual.copy(
                nome = atualizacao.nome ?: atual.nome,
                nfcTagId = atualizacao.nfcTagId ?: atual.nfcTagId,
                status = atualizacao.status ?: atual.status,
                categoria = atualizacao.categoria ?: atual.categoria
            )
            itens[idx] = atualizado
            return atualizado
        }

        override suspend fun excluir(id: UUID) {
            val idx = itens.indexOfFirst { it.id == id }
            if (idx == -1) throw EquipamentoNaoEncontrado(id.toString())
            itens[idx] = itens[idx].copy(status = StatusEquipamento.INATIVO)
        }
    }

    private class RepositorioEmprestimosFake : RepositorioDeEmprestimos {
        val itens = mutableListOf<Emprestimo>()

        override suspend fun listar(
            status: StatusEmprestimo?,
            usuarioId: UUID?,
            equipamentoId: UUID?,
            limit: Int,
            offset: Long
        ): List<Emprestimo> = itens.filter {
            (status == null || it.status == status) &&
                    (usuarioId == null || it.usuarioId == usuarioId) &&
                    (equipamentoId == null || it.equipamentoId == equipamentoId)
        }.drop(offset.toInt()).take(limit)

        override suspend fun buscarPorId(id: UUID): Emprestimo? = itens.find { it.id == id }

        override suspend fun buscarAtivoPorEquipamento(equipamentoId: UUID): Emprestimo? =
            itens.find { it.equipamentoId == equipamentoId && it.status == StatusEmprestimo.ATIVO }

        override suspend fun criar(
            usuarioId: UUID,
            equipamentoId: UUID,
            dataRetirada: String,
            dataDevolucaoPrevista: String
        ): Emprestimo {
            val criado = Emprestimo(
                id = UUID.randomUUID(),
                usuarioId = usuarioId,
                equipamentoId = equipamentoId,
                dataRetirada = dataRetirada,
                dataDevolucaoPrevista = dataDevolucaoPrevista,
                dataDevolucaoReal = null,
                status = StatusEmprestimo.ATIVO,
                devolvidoComAtraso = false
            )
            itens.add(criado)
            return criado
        }

        override suspend fun registrarDevolucao(
            emprestimoId: UUID,
            dataDevolucaoReal: String,
            devolvidoComAtraso: Boolean
        ): Emprestimo {
            val idx = itens.indexOfFirst { it.id == emprestimoId }
            if (idx == -1) throw EmprestimoNaoEncontrado(emprestimoId.toString())
            val atual = itens[idx]
            val devolvido = atual.copy(
                dataDevolucaoReal = dataDevolucaoReal,
                status = StatusEmprestimo.DEVOLVIDO,
                devolvidoComAtraso = devolvidoComAtraso
            )
            itens[idx] = devolvido
            return devolvido
        }
    }

    @Test
    fun `cenario 1 - retirada de equipamento disponivel via NFC com sucesso`() = testApplication {
        val repoEq = RepositorioEquipamentosFake()
        val repoEmp = RepositorioEmprestimosFake()

        // Cadastra um equipamento disponível
        val equipamento = repoEq.adicionar(
            NovoEquipamento(nome = "Osciloscópio Digital", nfcTagId = "NFC123", categoria = "ELETRONICA")
        )

        application {
            configureHttp()
            configureRouting(repoEq, repoEmp)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val usuarioId = UUID.randomUUID()
        val response = client.post("/api/v1/emprestimos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEmprestimo(usuarioId = usuarioId, nfcTagId = "NFC123"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.headers[HttpHeaders.Location]?.startsWith("/api/v1/emprestimos/") == true)
        val corpo = response.bodyAsText()
        assertTrue(corpo.contains("ATIVO"))
        assertTrue(corpo.contains("data_devolucao_prevista"))

        // O equipamento deve ter virado EMPRESTADO
        assertEquals(StatusEquipamento.EMPRESTADO, repoEq.buscarPorId(equipamento.id)?.status)
    }

    @Test
    fun `cenario 2 - erro 409 ao tentar retirar equipamento ja emprestado`() = testApplication {
        val repoEq = RepositorioEquipamentosFake()
        val repoEmp = RepositorioEmprestimosFake()

        val equipamento = repoEq.adicionar(
            NovoEquipamento(nome = "Kit Arduino", nfcTagId = "NFC456", categoria = "ROBOTICA")
        )
        // Marca como EMPRESTADO
        repoEq.atualizar(equipamento.id, AtualizarEquipamento(status = StatusEquipamento.EMPRESTADO))

        application {
            configureHttp()
            configureRouting(repoEq, repoEmp)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/v1/emprestimos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEmprestimo(usuarioId = UUID.randomUUID(), nfcTagId = "NFC456"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("equipamento-indisponivel"))
    }

    @Test
    fun `cenario 3 - devolucao de equipamento via NFC com sucesso`() = testApplication {
        val repoEq = RepositorioEquipamentosFake()
        val repoEmp = RepositorioEmprestimosFake()

        val equipamento = repoEq.adicionar(
            NovoEquipamento(nome = "Fonte Bancada", nfcTagId = "NFC789", categoria = "ELETRONICA")
        )
        // Realiza empréstimo ativo
        repoEq.atualizar(equipamento.id, AtualizarEquipamento(status = StatusEquipamento.EMPRESTADO))
        val emp = repoEmp.criar(
            usuarioId = UUID.randomUUID(),
            equipamentoId = equipamento.id,
            dataRetirada = "2026-10-15T10:00:00Z",
            dataDevolucaoPrevista = "2026-10-22T10:00:00Z"
        )

        application {
            configureHttp()
            configureRouting(repoEq, repoEmp)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/v1/emprestimos/devolucao") {
            contentType(ContentType.Application.Json)
            setBody(RegistrarDevolucao(nfcTagId = "NFC789"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        // O equipamento deve ter retornado para DISPONIVEL
        assertEquals(StatusEquipamento.DISPONIVEL, repoEq.buscarPorId(equipamento.id)?.status)
    }

    @Test
    fun `cenario 4 - erro 422 ao tentar devolver item nao emprestado`() = testApplication {
        val repoEq = RepositorioEquipamentosFake()
        val repoEmp = RepositorioEmprestimosFake()

        repoEq.adicionar(
            NovoEquipamento(nome = "Multímetro", nfcTagId = "NFC000", categoria = "ELETRONICA")
        )

        application {
            configureHttp()
            configureRouting(repoEq, repoEmp)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/v1/emprestimos/devolucao") {
            contentType(ContentType.Application.Json)
            setBody(RegistrarDevolucao(nfcTagId = "NFC000"))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(response.bodyAsText().contains("item-nao-emprestado"))
    }

    @Test
    fun `cenario 5 - consulta ao historico de emprestimos do equipamento via rota aninhada`() = testApplication {
        val repoEq = RepositorioEquipamentosFake()
        val repoEmp = RepositorioEmprestimosFake()

        val equipamento = repoEq.adicionar(
            NovoEquipamento(nome = "Gerador de Funções", nfcTagId = "NFC999", categoria = "ELETRONICA")
        )
        repoEmp.criar(
            usuarioId = UUID.randomUUID(),
            equipamentoId = equipamento.id,
            dataRetirada = "2026-10-10T10:00:00Z",
            dataDevolucaoPrevista = "2026-10-17T10:00:00Z"
        )

        application {
            configureHttp()
            configureRouting(repoEq, repoEmp)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.get("/api/v1/equipamentos/${equipamento.id}/emprestimos")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(equipamento.id.toString()))
    }
}
