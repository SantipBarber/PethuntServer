package com.pethunt.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pethunt.server.plugins.*
import com.pethunt.server.services.UserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Configurar plugins
    configureKoin()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()

    // Inicializar bases de datos
    configureDatabases()
}

fun Application.configureSecurity(userService: UserService) {
    install(Authentication) {
        // Autenticación básica
        basic("auth-basic") {
            realm = "PetHunt API"
            validate { credentials ->
                userService.validateCredentials(credentials)
            }
        }

        // Autenticación JWT (para tokens)
        jwt("auth-jwt") {
            realm = "PetHunt API"
            verifier(
                JWT.require(Algorithm.HMAC256("jwt-secret"))
                    .withAudience("https://pethunt.com")
                    .withIssuer("https://api.pethunt.com")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("id").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }

        // También podríamos añadir autenticación por formulario
        form("auth-form") {
            userParamName = "email"
            passwordParamName = "password"
            validate { credentials ->
                userService.validateCredentials(credentials)
            }
        }
    }
}