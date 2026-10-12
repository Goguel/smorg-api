package com.smorg.adapters.http

import com.smorg.domain.ConflitoTagNfc
import com.smorg.domain.EntradaInvalida
import com.smorg.domain.EquipamentoNaoEncontrado
import com.smorg.domain.OperacaoNaoPermitida
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Problem Details de acordo com a RFC 7807 / RFC 9457
@Serializable
data class Problema(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val violacoes: List<String>? = null
)

private val jsonFormat = Json {
    explicitNulls = false
    encodeDefaults = true
}

suspend fun ApplicationCall.responderProblema(
    status: HttpStatusCode,
    tipo: String,
    titulo: String,
    detalhe: String? = null,
    violacoes: List<String>? = null
) {
    val problema = Problema(
        type = "https://smorg.ufrn.br/errors/$tipo",
        title = titulo,
        status = status.value,
        detail = detalhe,
        instance = request.local.uri,
        violacoes = violacoes
    )
    respondText(
        text = jsonFormat.encodeToString(problema),
        contentType = ContentType.Application.ProblemJson,
        status = status
    )
}

fun Application.configurarTratamentoErros() {
    install(StatusPages) {
        exception<BadRequestException> { call, causa ->
            call.responderProblema(
                status = HttpStatusCode.BadRequest,
                tipo = "requisicao-malformada",
                titulo = "Requisição malformada",
                detalhe = causa.cause?.message ?: causa.message
            )
        }

        exception<NotFoundException> { call, causa ->
            call.responderProblema(
                status = HttpStatusCode.NotFound,
                tipo = "recurso-nao-encontrado",
                titulo = "Recurso não encontrado",
                detalhe = causa.message
            )
        }

        exception<EquipamentoNaoEncontrado> { call, causa ->
            call.responderProblema(
                status = HttpStatusCode.NotFound,
                tipo = "equipamento-nao-encontrado",
                titulo = "Equipamento não encontrado",
                detalhe = causa.message
            )
        }

        exception<EntradaInvalida> { call, causa ->
            call.responderProblema(
                status = HttpStatusCode.BadRequest,
                tipo = "dados-invalidos",
                titulo = "Dados de entrada inválidos",
                detalhe = "A requisição possui ${causa.violacoes.size} violação(ões) de validação.",
                violacoes = causa.violacoes
            )
        }

        exception<ConflitoTagNfc> { call, causa ->
            call.responderProblema(
                status = HttpStatusCode.Conflict,
                tipo = "conflito-tag-nfc",
                titulo = "Conflito de Tag NFC",
                detalhe = causa.message
            )
        }

        exception<OperacaoNaoPermitida> { call, causa ->
            call.responderProblema(
                status = HttpStatusCode.Conflict,
                tipo = "operacao-nao-permitida",
                titulo = "Operação não permitida",
                detalhe = causa.message
            )
        }

        exception<Throwable> { call, causa ->
            call.application.environment.log.error("Erro interno não tratado", causa)
            call.responderProblema(
                status = HttpStatusCode.InternalServerError,
                tipo = "erro-interno",
                titulo = "Erro interno do servidor",
                detalhe = "Ocorreu um erro inesperado no processamento da requisição."
            )
        }
    }
}
