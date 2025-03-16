package com.pethunt.server.config

import io.ktor.server.config.*
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory

class MongoFactory(
    private val config: ApplicationConfig
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val client: CoroutineClient by lazy {
        try {
            val mongoUri = config.property("database.mongodb.uri").getString()
            logger.info("Initializing MongoDB connection to $mongoUri")
            KMongo.createClient(mongoUri).coroutine
        } catch (e: Exception) {
            logger.error("Failed to initialize MongoDB client: ${e.message}", e)
            throw e
        }
    }

    val database: CoroutineDatabase by lazy {
        try {
            // Intenta leer el nombre de la BD desde la configuración con un valor por defecto
            val dbName = config.propertyOrNull("database.mongodb.database")?.getString() ?: "pethunt"
            logger.info("Using MongoDB database: $dbName")
            client.getDatabase(dbName)
        } catch (e: Exception) {
            logger.error("Failed to get MongoDB database: ${e.message}", e)
            throw e
        }
    }
}