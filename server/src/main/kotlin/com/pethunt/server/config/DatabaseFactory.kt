package com.pethunt.server.config

import com.pethunt.server.models.PetsTable
import com.pethunt.server.models.UsersTable
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.DriverManager

class DatabaseFactory(private val config: ApplicationConfig) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun init() {
        try {
            // Obtener configuración
            val host = config.property("database.postgres.host").getString()
            val port = config.property("database.postgres.port").getString()
            val dbName = config.property("database.postgres.database").getString()
            val user = config.property("database.postgres.user").getString()
            val password = config.property("database.postgres.password").getString()

            val autoCreate = config.propertyOrNull("database.postgres.auto_create")?.getString()?.toBoolean() ?: false

            if (autoCreate) {
                // Verificar si la base de datos existe
                val postgresJdbcUrl = "jdbc:postgresql://$host:$port/postgres"

                try {
                    // Conectar a la base de datos 'postgres' (default)
                    DriverManager.getConnection(postgresJdbcUrl, user, password).use { connection ->
                        // Comprobar si la base de datos 'pethunt' existe
                        val resultSet = connection.prepareStatement(
                            "SELECT 1 FROM pg_database WHERE datname = ?"
                        ).apply {
                            setString(1, dbName)
                        }.executeQuery()

                        if (!resultSet.next()) {
                            // La base de datos no existe, intentamos crearla
                            logger.info("Database '$dbName' not found, creating...")
                            connection.prepareStatement("CREATE DATABASE $dbName").execute()
                            logger.info("Database '$dbName' created successfully")
                        } else {
                            logger.info("Database '$dbName' already exists")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to check/create database: ${e.message}")
                    // Si no podemos crear la base de datos, logueamos el error pero continuamos intentando conectar
                }
            }

            // Conectar a la base de datos específica
            val jdbcURL = "jdbc:postgresql://$host:$port/$dbName"
            val database = Database.connect(
                url = jdbcURL,
                driver = "org.postgresql.Driver",
                user = user,
                password = password
            )

            // Inicializar tablas en la base de datos
            transaction(database) {
                // Asegurar que la extensión uuid-ossp está instalada en PostgreSQL
                try {
                    exec("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";")
                } catch (e: Exception) {
                    logger.warn("Could not create extension uuid-ossp: ${e.message}")
                    // Continuamos sin la extensión, aunque podría afectar la generación de UUIDs
                }

                // Crear todas las tablas necesarias
                SchemaUtils.create(UsersTable, PetsTable)
            }

            logger.info("Database initialization completed successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize database: ${e.message}")
            throw e
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}