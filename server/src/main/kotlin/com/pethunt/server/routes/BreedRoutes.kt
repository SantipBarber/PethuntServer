package com.pethunt.server.routes

import com.pethunt.server.models.Breed
import com.pethunt.server.services.BreedService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.breedRoutes(breedService: BreedService) {
    route("/breeds") {
        // Obtener todas las razas
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val speciesId = call.request.queryParameters["speciesId"]
            val query = call.request.queryParameters["query"]
            val size = call.request.queryParameters["size"]
            val temperament = call.request.queryParameters["temperament"]

            val result = when {
                !speciesId.isNullOrBlank() -> breedService.getBreedsBySpeciesId(speciesId, page, pageSize)
                !query.isNullOrBlank() -> breedService.searchBreeds(query, page, pageSize)
                size != null || temperament != null -> breedService.searchBreedsByCharacteristics(size, temperament, page, pageSize)
                else -> breedService.getAllBreeds(page, pageSize)
            }

            call.respond(result)
        }

        // Obtener una raza por ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))

            val breed = breedService.getBreedById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Raza no encontrada"))

            call.respond(breed)
        }

        // Rutas protegidas con autenticación JWT (solo para administradores)
        authenticate("auth-jwt") {
            // Crear una nueva raza
            post {
                // Verificar que el usuario es administrador (no implementado)

                try {
                    val breedDTO = call.receive<Breed>()
                    val breed = breedService.createBreed(breedDTO)
                    call.respond(HttpStatusCode.Created, breed)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error creating breed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }

            // Actualizar una raza
            put("/{id}") {
                // Verificar que el usuario es administrador (no implementado)

                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))

                try {
                    val breedDTO = call.receive<Breed>()
                    val breed = breedService.updateBreed(id, breedDTO)
                        ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Raza no encontrada"))

                    call.respond(breed)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error updating breed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }

            // Eliminar una raza
            delete("/{id}") {
                // Verificar que el usuario es administrador (no implementado)

                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))

                try {
                    val deleted = breedService.deleteBreed(id)

                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Raza no encontrada")))
                    }
                } catch (e: Exception) {
                    call.application.log.error("Error deleting breed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
        }
    }
}