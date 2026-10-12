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

class EquipamentoTest {

    private class RepositorioEquipamentosFake : RepositorioDeEquipamentos {
        val itens = mutableListOf<Equipamento>()

        override suspend fun listar(status: StatusEquipamento?, categoria: String?): List<Equipamento> =
            itens.filter { (status == null || it.status == status) && (categoria == null || it.categoria == categoria) }

        override suspend fun buscarPorId(id: UUID): Equipamento? =
            itens.find { it.id == id }

        override suspend fun buscarPorTag(nfcTagId: String): Equipamento? =
            itens.find { it.nfcTagId == nfcTagId }

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
            val index = itens.indexOfFirst { it.id == id }
            if (index == -1) throw EquipamentoNaoEncontrado(id.toString())
            val atual = itens[index]
            if (!atualizacao.nfcTagId.isNullOrBlank() && itens.any { it.nfcTagId == atualizacao.nfcTagId && it.id != id }) {
                throw ConflitoTagNfc(atualizacao.nfcTagId)
            }
            val atualizado = atual.copy(
                nome = atualizacao.nome ?: atual.nome,
                nfcTagId = atualizacao.nfcTagId ?: atual.nfcTagId,
                status = atualizacao.status ?: atual.status,
                categoria = atualizacao.categoria ?: atual.categoria
            )
            itens[index] = atualizado
            return atualizado
        }

        override suspend fun excluir(id: UUID) {
            val index = itens.indexOfFirst { it.id == id }
            if (index == -1) throw EquipamentoNaoEncontrado(id.toString())
            if (itens[index].status == StatusEquipamento.EMPRESTADO) {
                throw OperacaoNaoPermitida("Equipamento com status EMPRESTADO não pode ser removido ou inativado.")
            }
            itens[index] = itens[index].copy(status = StatusEquipamento.INATIVO)
        }
    }

    @Test
    fun `cenario 1 - cadastro de equipamento com sucesso`() = testApplication {
        val repo = RepositorioEquipamentosFake()
        application {
            configureHttp()
            configureRouting(repo)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/v1/equipamentos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEquipamento(nome = "Multímetro ET-1002", nfcTagId = "04A1B2C3D4", categoria = "ELETRONICA"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.headers[HttpHeaders.Location]?.startsWith("/api/v1/equipamentos/") == true)
        assertTrue(response.bodyAsText().contains("Multímetro ET-1002"))
        assertTrue(response.bodyAsText().contains("DISPONIVEL"))
    }

    @Test
    fun `cenario 2 - erro 409 ao cadastrar tag NFC duplicada`() = testApplication {
        val repo = RepositorioEquipamentosFake()
        application {
            configureHttp()
            configureRouting(repo)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        client.post("/api/v1/equipamentos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEquipamento(nome = "Item 1", nfcTagId = "TAG123", categoria = "ELETRONICA"))
        }

        val response = client.post("/api/v1/equipamentos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEquipamento(nome = "Item 2", nfcTagId = "TAG123", categoria = "ELETRONICA"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("conflito-tag-nfc"))
    }

    @Test
    fun `cenario 3 - validacao de campos obrigatorios`() = testApplication {
        val repo = RepositorioEquipamentosFake()
        application {
            configureHttp()
            configureRouting(repo)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/v1/equipamentos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEquipamento(nome = "", nfcTagId = null, categoria = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("dados-invalidos"))
    }

    @Test
    fun `cenario 4 - consulta por id e por tag NFC`() = testApplication {
        val repo = RepositorioEquipamentosFake()
        application {
            configureHttp()
            configureRouting(repo)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        // Cadastra um equipamento
        val postResponse = client.post("/api/v1/equipamentos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEquipamento(nome = "Osciloscópio Tektronix", nfcTagId = "OSC999", categoria = "ELETRONICA"))
        }
        val location = postResponse.headers[HttpHeaders.Location]!!

        // Busca por ID
        val getById = client.get(location)
        assertEquals(HttpStatusCode.OK, getById.status)
        assertTrue(getById.bodyAsText().contains("Osciloscópio Tektronix"))

        // Busca por Tag
        val getByTag = client.get("/api/v1/equipamentos/tag/OSC999")
        assertEquals(HttpStatusCode.OK, getByTag.status)
        assertTrue(getByTag.bodyAsText().contains("Osciloscópio Tektronix"))
    }

    @Test
    fun `cenario 5 - exclusao de equipamento disponivel`() = testApplication {
        val repo = RepositorioEquipamentosFake()
        application {
            configureHttp()
            configureRouting(repo)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val postResponse = client.post("/api/v1/equipamentos") {
            contentType(ContentType.Application.Json)
            setBody(NovoEquipamento(nome = "Fonte Bancada", nfcTagId = "FNT01", categoria = "ENERGIA"))
        }
        val location = postResponse.headers[HttpHeaders.Location]!!

        val deleteResponse = client.delete(location)
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verifica que o status mudou para INATIVO
        val getAfterDelete = client.get(location)
        assertTrue(getAfterDelete.bodyAsText().contains("INATIVO"))
    }
}
