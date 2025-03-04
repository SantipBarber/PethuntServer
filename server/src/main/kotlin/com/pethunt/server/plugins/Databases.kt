package com.pethunt.server.plugins

import com.pethunt.server.config.DatabaseFactory
import io.ktor.server.application.*

fun Application.configureDatabases() {
    val databaseFactory = DatabaseFactory(environment.config)
    databaseFactory.init()
}