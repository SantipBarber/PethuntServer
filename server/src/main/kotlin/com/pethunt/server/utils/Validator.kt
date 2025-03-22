package com.pethunt.server.utils

import com.pethunt.server.models.Breed
import com.pethunt.server.models.Species
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

/**
 * Clase de utilidad para validar modelos
 */
object ValidationUtils {

    /**
     * Valida una especie y devuelve una lista de errores o lista vacía si es válida
     */
    fun validateSpecies(species: Species): List<String> {
        val errors = mutableListOf<String>()

        if (species.type.isBlank()) {
            errors.add("El tipo de especie no puede estar vacío")
        } else if (!isValidSpeciesType(species.type)) {
            errors.add("Tipo de especie no válido: ${species.type}. Valores permitidos: DOG, CAT, BIRD, FISH, REPTILE, SMALL_MAMMAL, OTHER")
        }

        if (species.scientificName.isBlank()) {
            errors.add("El nombre científico no puede estar vacío")
        }

        if (species.commonNames.isEmpty()) {
            errors.add("Debe proporcionar al menos un nombre común")
        } else {
            species.commonNames.forEachIndexed { index, name ->
                if (name.language.isBlank()) {
                    errors.add("El idioma del nombre común #${index + 1} no puede estar vacío")
                }
                if (name.text.isBlank()) {
                    errors.add("El texto del nombre común #${index + 1} no puede estar vacío")
                }
            }
        }

        species.characteristics?.let { chars ->
            if (chars.size.isNotBlank() && !isValidSize(chars.size)) {
                errors.add("Tamaño no válido: ${chars.size}. Valores permitidos: TINY, SMALL, MEDIUM, LARGE, GIANT")
            }

            if (chars.lifespanMin < 0 || chars.lifespanMax < 0) {
                errors.add("La esperanza de vida no puede ser negativa")
            }

            if (chars.lifespanMin > chars.lifespanMax && chars.lifespanMax > 0) {
                errors.add("La esperanza de vida mínima no puede ser mayor que la máxima")
            }

            if (chars.weightMin < 0 || chars.weightMax < 0) {
                errors.add("El peso no puede ser negativo")
            }

            if (chars.weightMin > chars.weightMax && chars.weightMax > 0) {
                errors.add("El peso mínimo no puede ser mayor que el máximo")
            }
        }

        species.careInfo?.let { care ->
            if (care.exerciseNeeds < 1 || care.exerciseNeeds > 5) {
                errors.add("Las necesidades de ejercicio deben estar entre 1 y 5")
            }

            if (care.groomingNeeds < 1 || care.groomingNeeds > 5) {
                errors.add("Las necesidades de aseo deben estar entre 1 y 5")
            }

            if (care.trainingDifficulty < 1 || care.trainingDifficulty > 5) {
                errors.add("La dificultad de entrenamiento debe estar entre 1 y 5")
            }
        }

        return errors
    }

    fun validateBreed(breed: Breed): List<String> {
        val errors = mutableListOf<String>()

        if (breed.speciesId.isBlank()) {
            errors.add("El ID de la especie no puede estar vacío")
        }

        if (breed.names.isEmpty()) {
            errors.add("Debe proporcionar al menos un nombre para la raza")
        } else {
            breed.names.forEachIndexed { index, name ->
                if (name.language.isBlank()) {
                    errors.add("El idioma del nombre #${index + 1} no puede estar vacío")
                }
                if (name.text.isBlank()) {
                    errors.add("El texto del nombre #${index + 1} no puede estar vacío")
                }
            }
        }

        breed.characteristics?.let { chars ->
            if (chars.size.isNotBlank() && !isValidSize(chars.size)) {
                errors.add("Tamaño no válido: ${chars.size}. Valores permitidos: TINY, SMALL, MEDIUM, LARGE, GIANT")
            }

            chars.weightRange?.let { range ->
                if (range.min < 0 || range.max < 0) {
                    errors.add("El peso no puede ser negativo")
                }
                if (range.min > range.max) {
                    errors.add("El peso mínimo no puede ser mayor que el máximo")
                }
                if (range.unit.isBlank()) {
                    errors.add("La unidad de peso no puede estar vacía")
                }
            }

            chars.heightRange?.let { range ->
                if (range.min < 0 || range.max < 0) {
                    errors.add("La altura no puede ser negativa")
                }
                if (range.min > range.max) {
                    errors.add("La altura mínima no puede ser mayor que la máxima")
                }
                if (range.unit.isBlank()) {
                    errors.add("La unidad de altura no puede estar vacía")
                }
            }
        }

        return errors
    }


    suspend fun PipelineContext<Unit, ApplicationCall>.validateAndHandleSpecies(
        handler: suspend (Species) -> Unit
    ) {
        try {
            val species = call.receive<Species>()
            val errors = validateSpecies(species)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
                return
            }

            handler(species)
        } catch (e: ContentTransformationException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Formato de solicitud inválido: ${e.message}")
            )
        } catch (e: Exception) {
            call.application.log.error("Error procesando solicitud", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Error interno del servidor")
            )
        }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.validateAndHandleBreed(
        handler: suspend (Breed) -> Unit
    ) {
        try {
            val breed = call.receive<Breed>()
            val errors = validateBreed(breed)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
                return
            }

            handler(breed)
        } catch (e: ContentTransformationException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Formato de solicitud inválido: ${e.message}")
            )
        } catch (e: Exception) {
            call.application.log.error("Error procesando solicitud", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Error interno del servidor")
            )
        }
    }

    private fun isValidSpeciesType(type: String): Boolean {
        return setOf("DOG", "CAT", "BIRD", "FISH", "REPTILE", "SMALL_MAMMAL", "OTHER").contains(type.uppercase())
    }

    private fun isValidSize(size: String): Boolean {
        return setOf("TINY", "SMALL", "MEDIUM", "LARGE", "GIANT").contains(size.uppercase())
    }
}