package com.smorg.adapters.grpc

import com.smorg.domain.CalculadorDePrazos
import com.smorg.domain.DadosCalculoPrazo
import com.smorg.domain.PrazoCalculado
import com.smorg.prazos.v1.CalcularPrazoDevolucaoRequest
import com.smorg.prazos.v1.PrazoServiceGrpcKt.PrazoServiceCoroutineStub
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Adaptador da porta CalculadorDePrazos que se comunica com o microsserviço Go via gRPC.
 * É a única classe da API que conhece o contrato gRPC gerado.
 * Implementa deadline obrigatório e política de degradação graciosa (fallback).
 */
class CalculadorDePrazosGrpc(
    private val canal: ManagedChannel,
    private val prazo: Duration = 2.seconds
) : CalculadorDePrazos {

    private val stub = PrazoServiceCoroutineStub(canal)
    private val log = LoggerFactory.getLogger(CalculadorDePrazosGrpc::class.java)

    override suspend fun calcular(dados: DadosCalculoPrazo): PrazoCalculado {
        return try {
            val request = CalcularPrazoDevolucaoRequest.newBuilder()
                .setUsuarioId(dados.usuarioId.toString())
                .setPerfilUsuario(dados.perfilUsuario)
                .setEquipamentoId(dados.equipamentoId.toString())
                .apply {
                    dados.categoriaEquipamento?.let { setCategoriaEquipamento(it) }
                    dados.dataRetirada?.let { setDataRetirada(it) }
                }
                .build()

            val resposta = stub
                .withDeadlineAfter(prazo.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .calcularPrazoDevolucao(request)

            log.info(
                "Prazo calculado com sucesso via Go gRPC para usuário {}: {} dias concedidos",
                dados.usuarioId, resposta.diasConcedidos
            )

            PrazoCalculado(
                dataDevolucaoPrevista = resposta.dataDevolucaoPrevista,
                diasConcedidos = resposta.diasConcedidos,
                observacoes = resposta.observacoes
            )
        } catch (e: StatusException) {
            // UNAVAILABLE (Go fora), DEADLINE_EXCEEDED (timeout) etc.
            // A API não quebra: ativa a política de contingência/degradação graciosa.
            log.warn(
                "Serviço Go de prazos indisponível ou estourou prazo (status: {}). Ativando contingência.",
                e.status.code
            )
            aplicarFallback(dados)
        } catch (e: Exception) {
            log.error("Erro inesperado na comunicação gRPC com o serviço Go. Ativando contingência.", e)
            aplicarFallback(dados)
        }
    }

    private fun aplicarFallback(dados: DadosCalculoPrazo): PrazoCalculado {
        val dias = when (dados.perfilUsuario.uppercase()) {
            "MONITOR" -> 7L
            "PROFESSOR" -> 15L
            else -> 3L
        }
        val agora = Instant.now()
        val dataFinal = agora.plus(dias, ChronoUnit.DAYS).toString()

        return PrazoCalculado(
            dataDevolucaoPrevista = dataFinal,
            diasConcedidos = dias.toInt(),
            observacoes = "Fallback de contingência (serviço Go inacessível): $dias dias concedidos para ${dados.perfilUsuario}"
        )
    }

    fun fechar() {
        canal.shutdown().awaitTermination(1, TimeUnit.SECONDS)
    }

    companion object {
        fun para(endereco: String): CalculadorDePrazosGrpc {
            val canal = ManagedChannelBuilder.forTarget(endereco)
                .usePlaintext()
                .build()
            return CalculadorDePrazosGrpc(canal)
        }
    }
}

