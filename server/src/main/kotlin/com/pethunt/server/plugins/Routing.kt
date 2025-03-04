package com.pethunt.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.pethunt.server.routes.configureAuthRoutes
import com.pethunt.server.routes.configureUserRoutes

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            get("/health") {
                call.respond(mapOf("status" to "UP", "service" to "PetHunt API"))
            }

            // Aquí se registrarán más rutas posteriormente
            configureAuthRoutes()
            configureUserRoutes()
        }
    }
}
