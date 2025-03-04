package com.pethunt.server.plugins

import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import com.pethunt.server.di.appModule

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger(level = Level.INFO)
        modules(appModule)
    }
}