package com.pethunt.server.config

import com.pethunt.server.models.Users
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseFactory(private val config: ApplicationConfig) {

    fun init() {
        val jdbcURL = "jdbc:postgresql://${config.property("database.postgres.host").getString()}:${config.property("database.postgres.port").getString()}/${config.property("database.postgres.database").getString()}"
        val username = config.property("database.postgres.user").getString()
        val password = config.property("database.postgres.password").getString()

        val database = Database.connect(
            url = jdbcURL,
            driver = "org.postgresql.Driver",
            user = username,
            password = password
        )

        // Inicializar tablas en la base de datos
        transaction(database) {
            // Asegurar que la extensión uuid-ossp está instalada en PostgreSQL
            exec("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";")
            SchemaUtils.create(Users)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}