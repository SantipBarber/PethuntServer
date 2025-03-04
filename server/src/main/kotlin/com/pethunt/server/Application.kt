package com.pethunt.server

import com.pethunt.server.plugins.*
import io.ktor.server.application.*
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