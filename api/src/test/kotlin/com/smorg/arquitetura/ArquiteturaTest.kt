package com.smorg.arquitetura

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Testes de integridade da Clean Architecture via ArchUnit.
 * Verifica as fronteiras arquiteturais e a direção das dependências no bytecode compilado:
 *
 *     adaptadores (http, database, grpc, memoria) ──► dominio (Kotlin puro)
 */
class ArquiteturaTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.smorg")

    @Test
    fun `o dominio nao conhece framework, banco, gRPC nem adaptadores`() =
        regraDoDominio.check(classes)

    @Test
    fun `as rotas HTTP nao acessam o banco diretamente`() =
        noClasses().that().resideInAPackage("..adapters.http..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapters.database..", "org.jetbrains.exposed..", "java.sql..")
            .check(classes)

    @Test
    fun `a regra do dominio detecta violacao intencional`() {
        val fixture = ClassFileImporter().importPackages("com.smorg.arquitetura.violacao.domain")
        assertTrue(regraDoDominio.evaluate(fixture).hasViolation())
    }

    companion object {
        val regraDoDominio: ArchRule = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "io.ktor..",
                "org.jetbrains.exposed..",
                "java.sql..",
                "javax.sql..",
                "com.zaxxer..",
                "org.flywaydb..",
                "..adapters..",
                "io.grpc..",
                "com.google.protobuf..",
                "com.smorg.prazos.."
            )
            .because("o domínio é Kotlin puro: não pode depender de HTTP, banco de dados, gRPC ou adaptadores")
    }
}

