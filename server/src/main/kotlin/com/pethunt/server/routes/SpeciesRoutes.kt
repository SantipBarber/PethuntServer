package com.pethunt.server.routes

import com.pethunt.server.models.Species
import com.pethunt.server.services.SpeciesService
import com.pethunt.server.utils.PaginationUtils
import com.pethunt.server.utils.ValidationUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.speciesRoutes(speciesService: SpeciesService) {
    route("/species") {
        // Obtener todas las especies o buscar con filtros
        get {
            val paginationParams = PaginationUtils.extractPaginationParams(call)

            // Obtener el URL base para los enlaces de paginación
            val baseUrl = "${call.request.origin.scheme}://${call.request.host()}/species"

            // Parámetros de filtrado básico
            val type = call.request.queryParameters["type"]
            val query = call.request.queryParameters["query"]
            val size = call.request.queryParameters["size"]

            // Parámetros para búsqueda avanzada (pueden ser múltiples)
            val temperament = call.request.queryParameters.getAll("temperament")
            val dietTypes = call.request.queryParameters.getAll("dietType")
            val languages = call.request.queryParameters.getAll("language")

            // Determinar el tipo de búsqueda a realizar
            val result = when {
                // Si hay múltiples parámetros avanzados, usar búsqueda avanzada
                temperament != null || dietTypes != null || languages != null || size != null -> {
                    speciesService.advancedSearchSpecies(
                        query = query,
                        type = type,
                        temperament = temperament,
                        size = size,
                        dietTypes = dietTypes,
                        languages = languages,
                        paginationParams = paginationParams
                    )
                }
                // Si solo es por tipo
                !type.isNullOrBlank() -> {
                    speciesService.getSpeciesByType(type, paginationParams)
                }
                // Si solo es búsqueda por texto
                !query.isNullOrBlank() -> {
                    speciesService.searchSpecies(query, paginationParams)
                }
                // Por defecto, traer todas
                else -> {
                    speciesService.getAllSpecies(paginationParams)
                }
            }

            // Añadir URL base para los enlaces de paginación
            val responseWithLinks = result.copy(
                links = PaginationUtils.generatePaginationLinks(
                    baseUrl = baseUrl,
                    params = paginationParams,
                    totalPages = result.pagination.pages
                )
            )

            call.respond(responseWithLinks)
        }

        // Obtener una especie por ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "ID requerido")
            )

            val species = speciesService.getSpeciesById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Especie no encontrada"))

            call.respond(species)
        }

        // Crear una nueva especie (requiere autenticación)
        authenticate("auth-jwt") {
            post {
                try {
                    val species = call.receive<Species>()
                    val errors = ValidationUtils.validateSpecies(species)

                    if (errors.isNotEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
                        return@post
                    }

                    // Aquí puedes usar la función suspendida directamente
                    val createdSpecies = speciesService.createSpecies(species)
                    call.respond(HttpStatusCode.Created, createdSpecies)
                } catch (e: ContentTransformationException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Formato de solicitud inválido: ${e.message}")
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error procesando solicitud", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error interno del servidor")
                    )
                }
            }
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "ID requerido")
            )

            try {
                val species = call.receive<Species>()
                val errors = ValidationUtils.validateSpecies(species)

                if (errors.isNotEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
                    return@put
                }

                val updatedSpecies = speciesService.updateSpecies(id, species)
                    ?: return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Especie no encontrada")
                    )

                call.respond(updatedSpecies)
            } catch (e: ContentTransformationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Formato de solicitud inválido: ${e.message}")
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.application.log.error("Error updating species", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error interno del servidor")
                )
            }
        }

        // Eliminar una especie
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