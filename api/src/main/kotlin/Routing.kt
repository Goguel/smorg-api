package com.smorg

import com.smorg.adapters.database.RepositorioEmprestimosPostgres
import com.smorg.adapters.database.RepositorioEquipamentosPostgres
import com.smorg.adapters.grpc.CalculadorDePrazosGrpc
import com.smorg.adapters.http.emprestimoRotas
import com.smorg.adapters.http.equipamentoRotas
import com.smorg.domain.CalculadorDePrazos
import com.smorg.domain.RepositorioDeEmprestimos
import com.smorg.domain.RepositorioDeEquipamentos
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun obterCalculadorDePrazosPadrao(): CalculadorDePrazos {
    val endereco = System.getenv("PRAZOS_GRPC_HOST") ?: "localhost:50051"
    return CalculadorDePrazosGrpc.para(endereco)
}

fun Application.configureRouting(
    repositorioEquipamentos: RepositorioDeEquipamentos = RepositorioEquipamentosPostgres(),
    repositorioEmprestimos: RepositorioDeEmprestimos = RepositorioEmprestimosPostgres(),
    calculadorDePrazos: CalculadorDePrazos = obterCalculadorDePrazosPadrao()
) {
    routing {
        get("/") {
            call.respondText("SmOrg API ativa. Documentação Swagger disponível em /docs")
        }

        route("/api/v1/equipamentos") {
            equipamentoRotas(repositorioEquipamentos, repositorioEmprestimos)
        }

        route("/api/v1/emprestimos") {
            emprestimoRotas(repositorioEmprestimos, repositorioEquipamentos, calculadorDePrazos)
        }

        // Aliases para conveniência
        route("/equipamentos") {
            equipamentoRotas(repositorioEquipamentos, repositorioEmprestimos)
        }

        route("/emprestimos") {
            emprestimoRotas(repositorioEmprestimos, repositorioEquipamentos, calculadorDePrazos)
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