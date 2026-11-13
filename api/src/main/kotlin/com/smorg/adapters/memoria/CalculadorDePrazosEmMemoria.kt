package com.smorg.adapters.memoria

import com.smorg.domain.CalculadorDePrazos
import com.smorg.domain.DadosCalculoPrazo
import com.smorg.domain.PrazoCalculado
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Implementação em memória da porta CalculadorDePrazos.
 * Utilizada para testes unitários isolados e execução em modo de desenvolvimento local offline.
 */
class CalculadorDePrazosEmMemoria(
    private val diasAluno: Long = 3,
    private val diasMonitor: Long = 7,
    private val diasProfessor: Long = 15
) : CalculadorDePrazos {

    override suspend fun calcular(dados: DadosCalculoPrazo): PrazoCalculado {
        val dias = when (dados.perfilUsuario.uppercase()) {
            "MONITOR" -> diasMonitor
            "PROFESSOR" -> diasProfessor
            else -> diasAluno
        }

        val inicio = if (!dados.dataRetirada.isNullOrBlank()) {
            runCatching { Instant.parse(dados.dataRetirada) }.getOrElse { Instant.now() }
        } else {
            Instant.now()
        }

        val dataFinal = inicio.plus(dias, ChronoUnit.DAYS).toString()

        return PrazoCalculado(
            dataDevolucaoPrevista = dataFinal,
            diasConcedidos = dias.toInt(),
            observacoes = "Cálculo em memória simulado: $dias dias concedidos para o perfil ${dados.perfilUsuario}"
        )
    }
}

