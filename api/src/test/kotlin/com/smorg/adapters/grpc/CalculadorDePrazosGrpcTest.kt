package com.smorg.adapters.grpc

import com.smorg.domain.DadosCalculoPrazo
import com.smorg.prazos.v1.CalcularPrazoDevolucaoRequest
import com.smorg.prazos.v1.CalcularPrazoDevolucaoResponse
import com.smorg.prazos.v1.PrazoServiceGrpcKt.PrazoServiceCoroutineImplBase
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/**
 * Testes unitários do adaptador gRPC contra um servidor gRPC em memória (in-process).
 * Não depende de rede externa nem de container do Go em execução.
 */
class CalculadorDePrazosGrpcTest {

    private val servidores = mutableListOf<Server>()

    private fun clientePara(nome: String, servico: PrazoServiceCoroutineImplBase): CalculadorDePrazosGrpc {
        val server = InProcessServerBuilder.forName(nome)
            .directExecutor()
            .addService(servico)
            .build()
            .start()
        servidores.add(server)

        val channel = InProcessChannelBuilder.forName(nome)
            .directExecutor()
            .build()
        return CalculadorDePrazosGrpc(channel, prazo = 300.milliseconds)
    }

    @AfterTest
    fun finalizar() {
        servidores.forEach { it.shutdownNow() }
    }

    @Test
    fun `calculo bem sucedido via gRPC devolve resposta do servidor Go`() = runBlocking {
        val servicoMock = object : PrazoServiceCoroutineImplBase() {
            override suspend fun calcularPrazoDevolucao(request: CalcularPrazoDevolucaoRequest): CalcularPrazoDevolucaoResponse {
                return CalcularPrazoDevolucaoResponse.newBuilder()
                    .setDataDevolucaoPrevista("2026-11-10T23:59:59Z")
                    .setDiasConcedidos(3)
                    .setObservacoes("Regra de teste")
                    .build()
            }
        }

        val cliente = clientePara("servico-rapido", servicoMock)
        val resultado = cliente.calcular(
            DadosCalculoPrazo(
                usuarioId = UUID.randomUUID(),
                perfilUsuario = "ALUNO",
                equipamentoId = UUID.randomUUID()
            )
        )

        assertEquals("2026-11-10T23:59:59Z", resultado.dataDevolucaoPrevista)
        assertEquals(3, resultado.diasConcedidos)
        assertEquals("Regra de teste", resultado.observacoes)
    }

    @Test
    fun `servico lento estoura deadline e ativa fallback gracioso`() = runBlocking {
        val servicoLento = object : PrazoServiceCoroutineImplBase() {
            override suspend fun calcularPrazoDevolucao(request: CalcularPrazoDevolucaoRequest): CalcularPrazoDevolucaoResponse {
                delay(3_000) // Simula lentidão
                return CalcularPrazoDevolucaoResponse.newBuilder().build()
            }
        }

        val cliente = clientePara("servico-lento", servicoLento)
        var diasConcedidos = 0
        val duracao = measureTime {
            val resultado = cliente.calcular(
                DadosCalculoPrazo(
                    usuarioId = UUID.randomUUID(),
                    perfilUsuario = "MONITOR",
                    equipamentoId = UUID.randomUUID()
                )
            )
            diasConcedidos = resultado.diasConcedidos
        }

        // Deve retornar o fallback sem travar por 3 segundos
        assertTrue(duracao < 1_500.milliseconds, "esperou $duracao: timeout de deadline não foi respeitado")
        assertEquals(7, diasConcedidos) // Fallback para monitor concede 7 dias
    }

    @Test
    fun `servico fora do ar ativa fallback de contingencia sem derrubar a API`() = runBlocking {
        val channelInexistente = InProcessChannelBuilder.forName("servico-inexistente")
            .directExecutor()
            .build()
        val cliente = CalculadorDePrazosGrpc(channelInexistente, prazo = 200.milliseconds)

        val resultado = cliente.calcular(
            DadosCalculoPrazo(
                usuarioId = UUID.randomUUID(),
                perfilUsuario = "ALUNO",
                equipamentoId = UUID.randomUUID()
            )
        )

        assertEquals(3, resultado.diasConcedidos)
        assertTrue(resultado.observacoes.contains("Fallback de contingência"))
    }
}

