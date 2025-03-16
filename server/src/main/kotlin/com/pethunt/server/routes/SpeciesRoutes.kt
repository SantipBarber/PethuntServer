package com.pethunt.server.routes

import com.pethunt.server.models.Species
import com.pethunt.server.services.SpeciesService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.speciesRoutes(speciesService: SpeciesService) {
    route("/species") {
        // Obtener todas las especies
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val type = call.request.queryParameters["type"]
            val query = call.request.queryParameters["query"]

            val result = when {
                !type.isNullOrBlank() -> speciesService.getSpeciesByType(type, page, pageSize)
                !query.isNullOrBlank() -> speciesService.searchSpecies(query, page, pageSize)
                else -> speciesService.getAllSpecies(page, pageSize)
            }

            call.respond(result)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "ID requerido")
            )

            val species = speciesService.getSpeciesById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Especie no encontrada"))

            call.respond(species)
        }

        authenticate("auth-jwt") {
            post {
                try {
                    val speciesDTO = call.receive<Species>()
                    val species = speciesService.createSpecies(speciesDTO)
                    call.respond(HttpStatusCode.Created, species)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error creating species", e)
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "ID requerido")
            )

            try {
                val speciesDTO = call.receive<Species>()
                val species = speciesService.updateSpecies(id, speciesDTO)
                    ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Especie no encontrada"))

                call.respond(species)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.application.log.error("Error updating species", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "ID requerido")
            )

            try {
                val deleted = speciesService.deleteSpecies(id)

                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Especie no encontrada"))
                }
            } catch (e: Exception) {
                call.application.log.error("Error deleting species", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
            }
        }
    }
}