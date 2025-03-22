package com.pethunt.server.routes

import com.pethunt.server.config.MongoFactory
import com.pethunt.server.services.CacheMetricsService
import com.pethunt.server.services.StorageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

@Serializable
data class ServiceStatus(
    val name: String,
    val status: String
)

@Serializable
data class StatusResponse(
    val services: List<ServiceStatus>,
    val timestamp: Long,
    val version: String
)

fun Route.statusRoutes(mongoFactory: MongoFactory, storageService: StorageService) {
    val cacheMetricsService by inject<CacheMetricsService>()

    route("/status") {
        get {
            val services = mutableListOf<ServiceStatus>()

            // Verificar PostgreSQL
            try {
                transaction {
                    exec("SELECT 1")
                }
                services.add(ServiceStatus("postgresql", "UP"))
            } catch (e: Exception) {
                services.add(ServiceStatus("postgresql", "DOWN: ${e.message}"))
                application.log.error("PostgreSQL health check failed", e)
            }

            // Verificar MongoDB
            try {
                val database = mongoFactory.database
                database.listCollectionNames()
                services.add(ServiceStatus("mongodb", "UP"))
            } catch (e: Exception) {
                services.add(ServiceStatus("mongodb", "DOWN: ${e.message}"))
                application.log.error("MongoDB health check failed", e)
            }

            val response = StatusResponse(
                services = services,
                timestamp = System.currentTimeMillis(),
                version = "1.0.0"
            )

            call.respond(response)
        }
    }

    route("/status/cache") {
        get {
            try {
                call.respond(cacheMetricsService.getMetrics())
            } catch (e: Exception) {
                application.log.error("Cache metrics failed", e)
                call.respond(mapOf("error" to "Failed to get cache metrics: ${e.message}"))
            }
        }
    }

    route("/status/storage") {
        get {
            try {
                // Intentamos una operación simple para verificar que Firebase Storage funciona
                val testResult = storageService.testConnection()
                call.respond(mapOf(
                    "status" to "UP",
                    "bucketName" to testResult
                ))
            } catch (e: Exception) {
                application.log.error("Firebase Storage check failed", e)
                call.respond(
                    HttpStatusCode.InternalServerError, mapOf(
                    "status" to "DOWN",
                    "error" to e.message
                ))
            }
        }
    }
}
