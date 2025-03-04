package com.pethunt.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.pethunt.server.models.User
import com.pethunt.server.services.UserService
import org.jetbrains.exposed.sql.kotlin.datetime.Date
import org.koin.ktor.ext.inject
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: User)

fun Route.configureAuthRoutes() {
    val userService: UserService by inject()
    val config = application.environment.config

    val jwtSecret = config.property("jwt.secret").getString()
    val jwtIssuer = config.property("jwt.issuer").getString()
    val jwtAudience = config.property("jwt.audience").getString()

    route("/auth") {
        post("/login") {
            try {
                val loginRequest = call.receive<LoginRequest>()
                val user = userService.validateCredentials(loginRequest.email, loginRequest.password)

                if (user != null) {
                    // Generar token JWT
                    val expiresAt = Clock.System.now().plus(60.minutes).toEpochMilliseconds()
                    val token = JWT.create()
                        .withAudience(jwtAudience)
                        .withIssuer(jwtIssuer)
                        .withClaim("username", user.username)
                        .withClaim("userId", user.id)
                        .withExpiresAt(Date(expiresAt))
                        .sign(Algorithm.HMAC256(jwtSecret))

                    call.respond(LoginResponse(token, user))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred"))
            }
        }

        post("/register") {
            try {
                val userDTO = call.receive<com.pethunt.server.models.UserDTO>()
                val user = userService.createUser(userDTO)

                if (user != null) {
                    // Generar token JWT para nuevo usuario
                    val expiresAt = Clock.System.now().plus(60.minutes).toEpochMilliseconds()
                    val token = JWT.create()
                        .withAudience(jwtAudience)
                        .withIssuer(jwtIssuer)
                        .withClaim("username", user.username)
                        .withClaim("userId", user.id)
                        .withExpiresAt(Date(expiresAt))
                        .sign(Algorithm.HMAC256(jwtSecret))

                    call.respond(HttpStatusCode.Created, LoginResponse(token, user))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create user"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred"))
            }
        }
    }
}