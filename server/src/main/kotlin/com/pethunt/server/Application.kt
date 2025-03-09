package com.pethunt.server

import com.pethunt.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()

    configureDatabases()
}