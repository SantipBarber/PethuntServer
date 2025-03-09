package com.pethunt.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pethunt.server.models.UserCreateDTO
import com.pethunt.server.models.UserDTO
import com.pethunt.server.services.UserService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

fun Route.userRoutes(userService: UserService, jwtConfig: JwtConfig) {


    // Rutas públicas
    route("/users") {
        post("/register") {
            try {
                println("Recibida solicitud de registro")
                val userDTO = call.receive<UserCreateDTO>()
                println("Datos recibidos: $userDTO")
                val user = userService.createUser(userDTO)
                println("Usuario creado: $user")
                call.respond(HttpStatusCode.Created, user)
            } catch (e: Exception) {
                println("Error en registro: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error desconocido")))
            }
        }

        post("/login") {
            try {
                println("Recibida solicitud de login")
                val credentials = call.receive<LoginRequest>()
                println("Credenciales recibidas: ${credentials.email}")

                // Primero, obtener el usuario por email
                val user = userService.getUserByEmail(credentials.email)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Email o contraseña incorrectos"))

                // Luego validar la contraseña
                val principal = userService.validateCredentials(UserPasswordCredential(credentials.email, credentials.password))

                if (principal != null) {
                    println("Login exitoso para: ${credentials.email}")

                    // Generar token JWT
                    val token = jwtConfig.generateToken(user)

                    // Responder con un objeto JSON que contiene token y usuario
                    val response = LoginResponse(
                        token = token,
                        user = user
                    )

                    call.respond(HttpStatusCode.OK, response)
                } else {
                    println("Login fallido para: ${credentials.email} - Contraseña incorrecta")
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Email o contraseña incorrectos"))
                }
            } catch (e: Exception) {
                println("Error en login: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error interno del servidor")))
            }
        }
    }

    // Rutas protegidas con autenticación JWT
    authenticate("auth-jwt") {
        route("/users") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)

                if (userId != null) {
                    val user = userService.getUserById(UUID.fromString(userId))

                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)

                // Verificar que el usuario está actualizando su propio perfil
                if (userId != id) {
                    return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
                }

                try {
                    val userDTO = call.receive<UserCreateDTO>()
                    val user = userService.updateUser(UUID.fromString(id), userDTO)

                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }

            post("/change-password") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))

                try {
                    val request = call.receive<ChangePasswordRequest>()
                    val success = userService.changePassword(
                        UUID.fromString(userId),
                        request.currentPassword,
                        request.newPassword
                    )

                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Contraseña actualizada correctamente"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No se pudo actualizar la contraseña"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
        }
    }
}

class JwtConfig(private val secret: String, private val issuer: String, private val audience: String) {
    @OptIn(ExperimentalUuidApi::class)
    fun generateToken(user: UserDTO): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", user.id.toString())
            .withClaim("username", user.username)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hora
            .sign(Algorithm.HMAC256(secret))
    }
}

// Modelos para las solicitudes
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

// Clase para respuesta de login
@Serializable
data class LoginResponse(val token: String, val user: UserDTO)