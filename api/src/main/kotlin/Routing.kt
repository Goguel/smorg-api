package com.smorg

import com.smorg.adapters.database.RepositorioEquipamentosPostgres
import com.smorg.adapters.http.equipamentoRotas
import com.smorg.domain.RepositorioDeEquipamentos
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    repositorioEquipamentos: RepositorioDeEquipamentos = RepositorioEquipamentosPostgres()
) {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        route("/api/v1/equipamentos") {
            equipamentoRotas(repositorioEquipamentos)
        }

        route("/equipamentos") {
            equipamentoRotas(repositorioEquipamentos)
        }
    }
}