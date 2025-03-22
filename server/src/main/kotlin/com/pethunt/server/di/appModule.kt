package com.pethunt.server.di

import com.pethunt.server.config.DatabaseFactory
import com.pethunt.server.config.MongoFactory
import com.pethunt.server.config.RedisFactory
import com.pethunt.server.repositories.BreedRepository
import com.pethunt.server.repositories.PetRepository
import com.pethunt.server.repositories.SpeciesRepository
import com.pethunt.server.repositories.UserRepository
import com.pethunt.server.services.*
import io.ktor.server.config.*
import org.koin.dsl.module

val appModule = module {
    single { DatabaseFactory(get<ApplicationConfig>()) }
    single { MongoFactory(get<ApplicationConfig>()) }
    single { get<MongoFactory>().database }

    single { UserRepository() }
    single { PetRepository() }
    single { SpeciesRepository(get()) }
    single { BreedRepository(get()) }

    single { UserService(get()) }
    single { PetService(get()) }
    single { SpeciesService(get(), get()) }
    single { BreedService(get(), get(), get()) }

    single { RedisFactory(get<ApplicationConfig>()) }
    single { CacheService(get()) }
}