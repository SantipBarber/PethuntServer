package com.pethunt.server.routes

import com.pethunt.server.services.UserService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureUserRoutes() {
    val userService: UserService by inject()

    route("/users") {
        authenticate("auth-jwt") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)

                if (userId != null) {
                    val user = userService.getUserById(userId)
                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

                val user = userService.getUserById(id)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }
        }
    }
}