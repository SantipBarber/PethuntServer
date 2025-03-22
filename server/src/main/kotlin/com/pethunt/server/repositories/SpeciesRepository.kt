package com.pethunt.server.repositories

import com.pethunt.server.models.LocalizedText
import com.pethunt.server.models.Species
import com.pethunt.server.models.SpeciesCharacteristics
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import java.util.regex.Pattern

class SpeciesRepository(database: CoroutineDatabase) {
    private val collection = database.getCollection<Species>()

    suspend fun findAll(limit: Int = 100, skip: Int = 0): List<Species> {
        return collection.find()
            .limit(limit)
            .skip(skip)
            .toList()
    }

    suspend fun findById(id: String): Species? {
        return collection.findOneById(id)
    }

    suspend fun findByType(type: String, limit: Int = 100, skip: Int = 0): List<Species> {
        // Buscar coincidencia parcial e insensible a mayúsculas/minúsculas
        val pattern = Pattern.compile(type, Pattern.CASE_INSENSITIVE)
        return collection.find(Species::type regex pattern)
            .limit(limit)
            .skip(skip)
            .toList()
    }

    suspend fun search(query: String, limit: Int = 100, skip: Int = 0): List<Species> {
        val pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE)

        // Ampliamos la búsqueda para incluir más campos
        val filter = or(
            Species::commonNames.elemMatch(LocalizedText::text regex pattern),
            Species::descriptions.elemMatch(LocalizedText::text regex pattern),
            Species::scientificName regex pattern,
            Species::type regex pattern,
            Species::characteristics / SpeciesCharacteristics::temperament `in` listOf(query)
        )

        return collection.find(filter)
            .limit(limit)
            .skip(skip)
            .toList()
    }

    /**
     * Búsqueda avanzada con múltiples criterios
     */
    suspend fun advancedSearch(
        query: String? = null,
        type: String? = null,
        temperament: List<String>? = null,
        size: String? = null,
        dietTypes: List<String>? = null,
        languages: List<String>? = null,
        limit: Int = 100,
        skip: Int = 0
    ): List<Species> {
        val filters = mutableListOf<org.bson.conversions.Bson>()

        // Filtro de texto general
        query?.let {
            val pattern = Pattern.compile(it, Pattern.CASE_INSENSITIVE)
            filters.add(
                or(
                    Species::commonNames.elemMatch(LocalizedText::text regex pattern),
                    Species::descriptions.elemMatch(LocalizedText::text regex pattern),
                    Species::scientificName regex pattern
                )
            )
        }

        // Filtro por tipo
        type?.let {
            val pattern = Pattern.compile(it, Pattern.CASE_INSENSITIVE)
            filters.add(Species::type regex pattern)
        }

        // Filtro por temperamento
        temperament?.let {
            if (it.isNotEmpty()) {
                filters.add(Species::characteristics / SpeciesCharacteristics::temperament `in` it)
            }
        }

        // Filtro por tamaño
        size?.let {
            filters.add(Species::characteristics / SpeciesCharacteristics::size eq it)
        }

        // Filtro por tipo de dieta
        dietTypes?.let {
            if (it.isNotEmpty()) {
                filters.add(Species::characteristics / SpeciesCharacteristics::diet `in` it)
            }
        }

        // Filtro por idioma disponible
        languages?.let {
            if (it.isNotEmpty()) {
                filters.add(Species::commonNames.elemMatch(LocalizedText::language `in` it))
            }
        }

        val finalFilter = if (filters.isEmpty()) {
            null
        } else {
            and(*filters.toTypedArray())
        }

        return if (finalFilter != null) {
            collection.find(finalFilter)
                .limit(limit)
                .skip(skip)
                .toList()
        } else {
            findAll(limit, skip)
        }
    }

    suspend fun insert(species: Species): String {
        collection.insertOne(species)
        return species.id
    }

    suspend fun update(id: String, species: Species): Boolean {
        return collection.updateOneById(id, species).modifiedCount > 0
    }

    suspend fun delete(id: String): Boolean {
        try {
            val result = collection.deleteOneById(id)
            return result.deletedCount > 0
        } catch (e: Exception) {
            println("Error deleting species with ID $id: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    suspend fun count(): Long {
        return collection.countDocuments()
    }

    suspend fun countByType(type: String): Long {
        val pattern = Pattern.compile(type, Pattern.CASE_INSENSITIVE)
        return collection.countDocuments(Species::type regex pattern)
    }

    /**
     * Cuenta el número de documentos que coinciden con los criterios de búsqueda avanzada
     */
    suspend fun countAdvancedSearch(
        query: String? = null,
        type: String? = null,
        temperament: List<String>? = null,
        size: String? = null,
        dietTypes: List<String>? = null,
        languages: List<String>? = null
    ): Long {
        val filters = mutableListOf<org.bson.conversions.Bson>()

        query?.let {
            val pattern = Pattern.compile(it, Pattern.CASE_INSENSITIVE)
            filters.add(
                or(
                    Species::commonNames.elemMatch(LocalizedText::text regex pattern),
                    Species::descriptions.elemMatch(LocalizedText::text regex pattern),
                    Species::scientificName regex pattern
                )
            )
        }

        type?.let {
            val pattern = Pattern.compile(it, Pattern.CASE_INSENSITIVE)
            filters.add(Species::type regex pattern)
        }

        temperament?.let {
            if (it.isNotEmpty()) {
                filters.add(Species::characteristics / SpeciesCharacteristics::temperament `in` it)
            }
        }

        size?.let {
            filters.add(Species::characteristics / SpeciesCharacteristics::size eq it)
        }

        dietTypes?.let {
            if (it.isNotEmpty()) {
                filters.add(Species::characteristics / SpeciesCharacteristics::diet `in` it)
            }
        }

        languages?.let {
            if (it.isNotEmpty()) {
                filters.add(Species::commonNames.elemMatch(LocalizedText::language `in` it))
            }
        }

        val finalFilter = if (filters.isEmpty()) {
            null
        } else {
            and(*filters.toTypedArray())
        }

        return if (finalFilter != null) {
            collection.countDocuments(finalFilter)
        } else {
            count()
        }
    }
}