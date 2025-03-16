package com.pethunt.server.plugins

import io.ktor.server.application.*
import org.koin.core.logger.Level
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import com.pethunt.server.di.appModule
import io.ktor.server.config.*
import org.koin.dsl.module
import org.koin.environmentProperties

fun Application.configureKoin() {
    val appConfig = environment.config

    install(Koin) {
        slf4jLogger(level = Level.INFO)
        modules(
            module { single<ApplicationConfig> { appConfig } },
            appModule
        )
        environmentProperties()
    }
}