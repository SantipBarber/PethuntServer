package com.pethunt.server.routes

import com.pethunt.server.models.Breed
import com.pethunt.server.services.BreedService
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

fun Route.breedRoutes(breedService: BreedService) {
    route("/breeds") {
        // Obtener todas las razas con filtros opcionales
        get {
            val paginationParams = PaginationUtils.extractPaginationParams(call)

            // Obtener el URL base para los enlaces de paginación
            val baseUrl = "${call.request.origin.scheme}://${call.request.host()}/breeds"

            // Parámetros de filtrado básico
            val speciesId = call.request.queryParameters["speciesId"]
            val query = call.request.queryParameters["query"]
            val size = call.request.queryParameters["size"]
            val temperament = call.request.queryParameters["temperament"]

            // Parámetros para búsqueda avanzada
            val temperamentList = call.request.queryParameters.getAll("temperaments")
            val colors = call.request.queryParameters.getAll("colors")
            val coatTypes = call.request.queryParameters.getAll("coatTypes")
            val languages = call.request.queryParameters.getAll("languages")
            val minWeight = call.request.queryParameters["minWeight"]?.toDoubleOrNull()
            val maxWeight = call.request.queryParameters["maxWeight"]?.toDoubleOrNull()

            // Determinar el tipo de búsqueda a realizar
            val result = when {
                // Si hay parámetros de búsqueda avanzada, usar esa función
                temperamentList != null || colors != null || coatTypes != null ||
                        languages != null || minWeight != null || maxWeight != null -> {
                    breedService.advancedSearchBreeds(
                        query = query,
                        speciesId = speciesId,
                        size = size,
                        temperament = temperamentList,
                        colors = colors,
                        coatTypes = coatTypes,
                        languages = languages,
                        minWeight = minWeight,
                        maxWeight = maxWeight,
                        paginationParams = paginationParams
                    )
                }
                // Si se busca por especie
                !speciesId.isNullOrBlank() -> {
                    breedService.getBreedsBySpeciesId(speciesId, paginationParams)
                }
                // Si se busca por texto
                !query.isNullOrBlank() -> {
                    breedService.searchBreeds(query, paginationParams)
                }
                // Si se busca por características básicas
                size != null || temperament != null -> {
                    breedService.searchBreedsByCharacteristics(size, temperament, paginationParams)
                }
                // Por defecto, traer todas
                else -> {
                    breedService.getAllBreeds(paginationParams)
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

        // Obtener una raza por ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "ID requerido")
            )

            val breed = breedService.getBreedById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Raza no encontrada"))

            call.respond(breed)
        }

        // Rutas protegidas con autenticación JWT (solo para administradores)
        authenticate("auth-jwt") {
            // Crear una nueva raza
            post {
                try {
                    val breed = call.receive<Breed>()
                    val errors = ValidationUtils.validateBreed(breed)

                    if (errors.isNotEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
                        return@post
                    }

                    val createdBreed = breedService.createBreed(breed)
                    call.respond(HttpStatusCode.Created, createdBreed)
                } catch (e: ContentTransformationException) {
                    call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Formato de solicitud inválido: ${e.message}"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error creating breed", e)
                    call.respond(HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error interno del servidor"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "ID requerido")
                )

                try {
                    val breed = call.receive<Breed>()
                    val errors = ValidationUtils.validateBreed(breed)

                    if (errors.isNotEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
                        return@put
                    }

                    val updatedBreed = breedService.updateBreed(id, breed)
                        ?: return@put call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Raza no encontrada")
                        )

                    call.respond(updatedBreed)
                } catch (e: ContentTransformationException) {
                    call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Formato de solicitud inválido: ${e.message}"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error updating breed", e)
                    call.respond(HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error interno del servidor"))
                }
            }

            // Eliminar una raza
            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "ID requerido")
                )

                try {
                    val deleted = breedService.deleteBreed(id)

                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Raza no encontrada"))
                    }
                } catch (e: Exception) {
                    call.application.log.error("Error deleting breed", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error interno del servidor")
                    )
                }
            }
        }
    }
}