package com.smorg

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        // Usa uma configuração vazia para evitar carregar o application.yaml e tentar conectar no banco
        environment {
            config = io.ktor.server.config.MapApplicationConfig()
        }
        application {
            configureHttp()
            configureRouting()
        }
        // verify server root returns 200
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

}
