package com.pethunt.server.di

import com.pethunt.server.config.DatabaseFactory
import com.pethunt.server.repositories.UserRepository
import com.pethunt.server.services.UserService
import io.ktor.server.application.*
import org.koin.dsl.module

val appModule = module {
    // Configuración
    single { DatabaseFactory(get<Application>().environment.config) }
    
    // Repositorios
    single { UserRepository(get()) }
    
    // Servicios
    single { UserService(get()) }
}