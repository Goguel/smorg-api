package com.smorg.adapters.http

import com.smorg.domain.*
import io.ktor.http.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

fun Route.emprestimoRotas(
    repositorioEmprestimos: RepositorioDeEmprestimos,
    repositorioEquipamentos: RepositorioDeEquipamentos
) {
    // Listar empréstimos com filtros e paginação
    get {
        val statusParam = call.request.queryParameters["status"]?.let {
            runCatching { StatusEmprestimo.valueOf(it.uppercase()) }.getOrElse {
                throw BadRequestException("Status inválido '$it'. Valores válidos: ${StatusEmprestimo.entries.joinToString()}")
            }
        }
        val usuarioIdParam = call.request.queryParameters["usuario_id"]?.let {
            runCatching { UUID.fromString(it) }.getOrElse {
                throw BadRequestException("usuario_id inválido: não é um UUID válido")
            }
        }
        val equipamentoIdParam = call.request.queryParameters["equipamento_id"]?.let {
            runCatching { UUID.fromString(it) }.getOrElse {
                throw BadRequestException("equipamento_id inválido: não é um UUID válido")
            }
        }

        val limit = (call.request.queryParameters["size"]?.toIntOrNull()
            ?: call.request.queryParameters["limit"]?.toIntOrNull()
            ?: 20).coerceIn(1, 100)
        val offset = call.request.queryParameters["page"]?.toLongOrNull()?.let { it * limit }
            ?: call.request.queryParameters["offset"]?.toLongOrNull()
            ?: 0L

        val emprestimos = repositorioEmprestimos.listar(
            status = statusParam,
            usuarioId = usuarioIdParam,
            equipamentoId = equipamentoIdParam,
            limit = limit,
            offset = offset
        )
        call.respond(HttpStatusCode.OK, emprestimos)
    }

    // Registrar retirada (abertura de empréstimo)
    post {
        val novo = call.receive<NovoEmprestimo>()
        val violacoes = novo.violacoes()
        if (violacoes.isNotEmpty()) {
            throw EntradaInvalida(violacoes)
        }

        val equipamento = if (novo.equipamentoId != null) {
            repositorioEquipamentos.buscarPorId(novo.equipamentoId)
                ?: throw EquipamentoNaoEncontrado(novo.equipamentoId.toString())
        } else {
            repositorioEquipamentos.buscarPorTag(novo.nfcTagId!!)
                ?: throw EquipamentoNaoEncontrado(novo.nfcTagId)
        }

        if (equipamento.status != StatusEquipamento.DISPONIVEL) {
            throw EquipamentoIndisponivel(equipamento.nome, equipamento.status)
        }

        val agora = Instant.now()
        val dataRetirada = agora.toString()
        val dataDevolucaoPrevista = agora.plus(7, ChronoUnit.DAYS).toString()

        val criado = repositorioEmprestimos.criar(
            usuarioId = novo.usuarioId,
            equipamentoId = equipamento.id,
            dataRetirada = dataRetirada,
            dataDevolucaoPrevista = dataDevolucaoPrevista
        )

        // Atualiza estado do equipamento para EMPRESTADO
        repositorioEquipamentos.atualizar(
            id = equipamento.id,
            atualizacao = AtualizarEquipamento(status = StatusEquipamento.EMPRESTADO)
        )

        val resultado = criado.copy(equipamento = equipamento.copy(status = StatusEquipamento.EMPRESTADO))
        call.response.header(HttpHeaders.Location, "/api/v1/emprestimos/${criado.id}")
        call.respond(HttpStatusCode.Created, resultado)
    }

    // Registrar devolução
    post("/devolucao") {
        val requisicao = call.receive<RegistrarDevolucao>()
        val violacoes = requisicao.violacoes()
        if (violacoes.isNotEmpty()) {
            throw EntradaInvalida(violacoes)
        }

        val equipamentoIdAlvo: UUID = when {
            requisicao.emprestimoId != null -> {
                val emp = repositorioEmprestimos.buscarPorId(requisicao.emprestimoId)
                    ?: throw EmprestimoNaoEncontrado(requisicao.emprestimoId.toString())
                emp.equipamentoId
            }
            requisicao.equipamentoId != null -> requisicao.equipamentoId
            else -> {
                val eq = repositorioEquipamentos.buscarPorTag(requisicao.nfcTagId!!)
                    ?: throw EquipamentoNaoEncontrado(requisicao.nfcTagId)
                eq.id
            }
        }

        val emprestimoAtivo = repositorioEmprestimos.buscarAtivoPorEquipamento(equipamentoIdAlvo)
            ?: throw ItemNaoEmprestado(equipamentoIdAlvo.toString())

        val agora = Instant.now()
        val dataReal = agora.toString()
        val prazoPrevisto = runCatching { Instant.parse(emprestimoAtivo.dataDevolucaoPrevista) }.getOrNull()
        val atrasado = prazoPrevisto?.let { agora.isAfter(it) } ?: false

        val atualizado = repositorioEmprestimos.registrarDevolucao(
            emprestimoId = emprestimoAtivo.id,
            dataDevolucaoReal = dataReal,
            devolvidoComAtraso = atrasado
        )

        // Devolve o equipamento para DISPONIVEL
        val eqAtualizado = repositorioEquipamentos.atualizar(
            id = equipamentoIdAlvo,
            atualizacao = AtualizarEquipamento(status = StatusEquipamento.DISPONIVEL)
        )

        call.respond(HttpStatusCode.OK, atualizado.copy(equipamento = eqAtualizado))
    }

    // Consulta por ID
    get("/{id}") {
        val id = extrairUUID(call.parameters["id"])
        val emprestimo = repositorioEmprestimos.buscarPorId(id)
            ?: throw EmprestimoNaoEncontrado(id.toString())
        val equipamento = repositorioEquipamentos.buscarPorId(emprestimo.equipamentoId)
        call.respond(HttpStatusCode.OK, emprestimo.copy(equipamento = equipamento))
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
