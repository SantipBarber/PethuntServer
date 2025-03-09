package com.pethunt.server.plugins

import com.pethunt.server.routes.JwtConfig
import com.pethunt.server.routes.userRoutes
import com.pethunt.server.services.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get


fun Application.configureRouting() {
    val userService = this.get<UserService>()
    val jwtConfig = JwtConfig(
        environment.config.property("jwt.secret").getString(),
        environment.config.property("jwt.issuer").getString(),
        environment.config.property("jwt.audience").getString()
    )
    install(RoutingRoot) {
        userRoutes(userService, jwtConfig)
    }
}
