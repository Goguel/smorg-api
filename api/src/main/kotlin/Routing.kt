package com.smorg

import com.smorg.adapters.database.RepositorioEmprestimosPostgres
import com.smorg.adapters.database.RepositorioEquipamentosPostgres
import com.smorg.adapters.http.emprestimoRotas
import com.smorg.adapters.http.equipamentoRotas
import com.smorg.domain.RepositorioDeEmprestimos
import com.smorg.domain.RepositorioDeEquipamentos
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    repositorioEquipamentos: RepositorioDeEquipamentos = RepositorioEquipamentosPostgres(),
    repositorioEmprestimos: RepositorioDeEmprestimos = RepositorioEmprestimosPostgres()
) {
    routing {
        get("/") {
            call.respondText("SmOrg API ativa. Documentação Swagger disponível em /docs")
        }

        route("/api/v1/equipamentos") {
            equipamentoRotas(repositorioEquipamentos, repositorioEmprestimos)
        }

        route("/api/v1/emprestimos") {
            emprestimoRotas(repositorioEmprestimos, repositorioEquipamentos)
        }

        // Aliases para conveniência
        route("/equipamentos") {
            equipamentoRotas(repositorioEquipamentos, repositorioEmprestimos)
        }

        route("/emprestimos") {
            emprestimoRotas(repositorioEmprestimos, repositorioEquipamentos)
        }

        // OpenAPI e Swagger UI interativo
        swaggerUI(path = "docs") {
            info = OpenApiInfo(
                title = "SmOrg API - Gestão de Ativos por NFC",
                version = "1.0.0",
                description = "API para catálogo de ativos laboratoriais e controle de empréstimos e devoluções via NFC."
            )
        }
    }
}