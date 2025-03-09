package com.pethunt.server.di

import com.pethunt.server.config.DatabaseFactory
import com.pethunt.server.repositories.PetRepository
import com.pethunt.server.repositories.UserRepository
import com.pethunt.server.services.PetService
import com.pethunt.server.services.UserService
import io.ktor.server.application.*
import org.koin.dsl.module

val appModule = module {
    single { DatabaseFactory(get<Application>().environment.config) }

    single { UserRepository() }
    single { PetRepository() }

    single { UserService(get()) }
    single { PetService(get()) }
}