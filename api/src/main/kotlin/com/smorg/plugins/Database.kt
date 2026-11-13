package com.smorg.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun inicializarBanco(dbUrl: String, dbUser: String, dbPassword: String): HikariDataSource {
    // 1. Executa as migrações Flyway versionadas (V1, V2)
    val flyway = Flyway.configure()
        .dataSource(dbUrl, dbUser, dbPassword)
        .load()
    flyway.migrate()

    // 2. Conecta o Exposed via pool de conexões HikariCP
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 5
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)
    return dataSource
}

fun Application.configureDatabases() {
    val dbUrl = System.getenv("POSTGRES_JDBC_URL")
        ?: System.getenv("STORAGE_JDBCURL")
        ?: environment.config.propertyOrNull("storage.jdbcURL")?.getString()
        ?: "jdbc:postgresql://localhost:5432/smorg"

    val dbUser = System.getenv("POSTGRES_USER")
        ?: System.getenv("STORAGE_USER")
        ?: environment.config.propertyOrNull("storage.user")?.getString()
        ?: "smorg"

    val dbPassword = System.getenv("POSTGRES_PASSWORD")
        ?: System.getenv("STORAGE_PASSWORD")
        ?: environment.config.propertyOrNull("storage.password")?.getString()
        ?: "smorg"

    inicializarBanco(dbUrl, dbUser, dbPassword)
    log.info("Banco de dados PostgreSQL conectado e migrações Flyway aplicadas com sucesso!")
}
