package com.pethunt.server.plugins

import com.pethunt.server.config.DatabaseFactory
import com.pethunt.server.config.RedisFactory
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val databaseFactory = DatabaseFactory(environment.config)
    databaseFactory.init()

    val redisFactory: RedisFactory by inject()
    if (!redisFactory.testConnection()) {
        log.warn("Redis connection test failed - cache will not be available")
    } else {
        log.info("Redis connection successful")
    }
}
