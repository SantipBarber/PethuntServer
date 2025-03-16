package com.pethunt.server.plugins

import com.pethunt.server.config.MongoFactory
import com.pethunt.server.routes.*
import com.pethunt.server.services.BreedService
import com.pethunt.server.services.PetService
import com.pethunt.server.services.SpeciesService
import com.pethunt.server.services.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get


fun Application.configureRouting() {
    val userService = get<UserService>()
    val petService = get<PetService>()
    val speciesService = get<SpeciesService>()
    val breedService = get<BreedService>()
    val mongoFactory = get<MongoFactory>()

    val jwtConfig = JwtConfig(
        environment.config.property("jwt.secret").getString(),
        environment.config.property("jwt.issuer").getString(),
        environment.config.property("jwt.audience").getString()
    )

    install(RoutingRoot) {
        userRoutes(userService, jwtConfig)
        petRoutes(petService)
        speciesRoutes(speciesService)
        breedRoutes(breedService)
        statusRoutes( mongoFactory)
    }
}
