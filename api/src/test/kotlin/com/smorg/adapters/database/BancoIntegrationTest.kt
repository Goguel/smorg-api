package com.smorg.adapters.database

import com.smorg.domain.NovoEquipamento
import com.smorg.domain.StatusEquipamento
import com.smorg.domain.StatusEmprestimo
import com.smorg.plugins.inicializarBanco
import kotlinx.coroutines.runBlocking
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID
import kotlin.test.*

/**
 * Testes de integração com banco de dados PostgreSQL real utilizando Testcontainers.
 * Valida a execução das migrações Flyway e a integridade das operações de persistência e chaves estrangeiras.
 */
class BancoIntegrationTest {

    companion object {
        // PostgreSQL descartável em container para testes de integração
        private val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            start()
            inicializarBanco(jdbcUrl, username, password)
        }
    }

    private val repoEquipamentos = RepositorioEquipamentosPostgres()
    private val repoEmprestimos = RepositorioEmprestimosPostgres()

    @Test
    fun `persiste equipamento e busca por tag NFC no banco real`() = runBlocking {
        val tag = "NFC-TEST-${UUID.randomUUID().toString().take(8)}"
        val criado = repoEquipamentos.adicionar(
            NovoEquipamento(
                nome = "Multímetro Minipa",
                nfcTagId = tag,
                categoria = "ELETRONICA"
            )
        )

        val encontrado = repoEquipamentos.buscarPorTag(tag)
        assertNotNull(encontrado)
        assertEquals(criado.id, encontrado.id)
        assertEquals("Multímetro Minipa", encontrado.nome)
        assertEquals(StatusEquipamento.DISPONIVEL, encontrado.status)
    }

    @Test
    fun `fluxo completo de emprestimo e devolucao com integridade relacional`() = runBlocking {
        val equipamento = repoEquipamentos.adicionar(
            NovoEquipamento(
                nome = "Gerador de Sinais",
                nfcTagId = "NFC-GER-${UUID.randomUUID().toString().take(8)}",
                categoria = "ELETRONICA"
            )
        )

        val usuarioId = UUID.randomUUID()
        val emprestimo = repoEmprestimos.criar(
            usuarioId = usuarioId,
            equipamentoId = equipamento.id,
            dataRetirada = "2026-11-01T10:00:00Z",
            dataDevolucaoPrevista = "2026-11-04T23:59:59Z"
        )

        assertEquals(StatusEmprestimo.ATIVO, emprestimo.status)
        assertEquals(usuarioId, emprestimo.usuarioId)

        // Registrar devolução
        val devolvido = repoEmprestimos.registrarDevolucao(
            emprestimoId = emprestimo.id,
            dataDevolucaoReal = "2026-11-03T16:00:00Z",
            devolvidoComAtraso = false
        )

        assertEquals(StatusEmprestimo.DEVOLVIDO, devolvido.status)
        assertFalse(devolvido.devolvidoComAtraso)
        assertNotNull(devolvido.dataDevolucaoReal)
    }
}

