package com.smorg.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val dbUrl = environment.config.propertyOrNull("storage.jdbcURL")?.getString() ?: "jdbc:postgresql://localhost:5432/smorg"
    val dbUser = environment.config.propertyOrNull("storage.user")?.getString() ?: "smorg"
    val dbPassword = environment.config.propertyOrNull("storage.password")?.getString() ?: "smorg"

    // 1. Run Flyway Migrations
    val flyway = Flyway.configure()
        .dataSource(dbUrl, dbUser, dbPassword)
        .load()
    flyway.migrate()

    // 2. Connect Exposed via HikariCP
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    
    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)
    
    log.info("Database connected and migrations applied successfully!")
}
