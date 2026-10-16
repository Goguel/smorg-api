package com.smorg.adapters.http

import com.smorg.domain.*
import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.equipamentoRotas(
    repositorio: RepositorioDeEquipamentos,
    repositorioEmprestimos: RepositorioDeEmprestimos? = null
) {
    // Listar e cadastrar
    get {
        val statusParam = call.request.queryParameters["status"]?.let {
            runCatching { StatusEquipamento.valueOf(it.uppercase()) }.getOrElse {
                throw BadRequestException("Status inválido '$it'. Valores válidos: ${StatusEquipamento.entries.joinToString()}")
            }
        }
        val categoriaParam = call.request.queryParameters["categoria"]
        val limit = (call.request.queryParameters["size"]?.toIntOrNull()
            ?: call.request.queryParameters["limit"]?.toIntOrNull()
            ?: 20).coerceIn(1, 100)
        val offset = call.request.queryParameters["page"]?.toLongOrNull()?.let { it * limit }
            ?: call.request.queryParameters["offset"]?.toLongOrNull()
            ?: 0L

        val equipamentos = repositorio.listar(statusParam, categoriaParam, limit, offset)
        call.respond(HttpStatusCode.OK, equipamentos)
    }

    post {
        val novo = call.receive<NovoEquipamento>()
        val violacoes = novo.violacoes()
        if (violacoes.isNotEmpty()) {
            throw EntradaInvalida(violacoes)
        }

        val criado = repositorio.adicionar(novo)
        call.response.header(HttpHeaders.Location, "/api/v1/equipamentos/${criado.id}")
        call.respond(HttpStatusCode.Created, criado)
    }

    // Busca específica por tag NFC
    get("/tag/{tag}") {
        val tag = call.parameters["tag"] ?: throw BadRequestException("Tag NFC não informada")
        val equipamento = repositorio.buscarPorTag(tag) ?: throw EquipamentoNaoEncontrado(tag)
        call.respond(HttpStatusCode.OK, equipamento)
    }

    // Operações por ID
    route("/{id}") {
        get {
            val id = extrairUUID(call.parameters["id"])
            val equipamento = repositorio.buscarPorId(id) ?: throw EquipamentoNaoEncontrado(id.toString())
            call.respond(HttpStatusCode.OK, equipamento)
        }

        put {
            val id = extrairUUID(call.parameters["id"])
            val atualizacao = call.receive<AtualizarEquipamento>()
            val violacoes = atualizacao.violacoes()
            if (violacoes.isNotEmpty()) {
                throw EntradaInvalida(violacoes)
            }

            val atualizado = repositorio.atualizar(id, atualizacao)
            call.respond(HttpStatusCode.OK, atualizado)
        }

        delete {
            val id = extrairUUID(call.parameters["id"])
            repositorio.excluir(id)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/emprestimos") {
            val id = extrairUUID(call.parameters["id"])
            val limit = (call.request.queryParameters["size"]?.toIntOrNull()
                ?: call.request.queryParameters["limit"]?.toIntOrNull()
                ?: 20).coerceIn(1, 100)
            val offset = call.request.queryParameters["page"]?.toLongOrNull()?.let { it * limit }
                ?: call.request.queryParameters["offset"]?.toLongOrNull()
                ?: 0L
            val emprestimos = repositorioEmprestimos?.listar(
                equipamentoId = id,
                limit = limit,
                offset = offset
            ) ?: emptyList()
            call.respond(HttpStatusCode.OK, emprestimos)
        }
    }
}

private fun extrairUUID(param: String?): UUID {
    if (param.isNullOrBlank()) {
        throw BadRequestException("Identificador (ID) ausente na requisição")
    }
    return runCatching { UUID.fromString(param) }.getOrElse {
        throw BadRequestException("Identificador inválido: '$param' não é um UUID válido")
    }
}
