package com.pethunt.server.repositories

import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.lt
import com.pethunt.server.models.Breed
import com.pethunt.server.models.BreedCharacteristics
import com.pethunt.server.models.LocalizedText
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import java.util.regex.Pattern

class BreedRepository(private val database: CoroutineDatabase) {
    private val collection = database.getCollection<Breed>()

    suspend fun findAll(limit: Int = 100, skip: Int = 0): List<Breed> {
        return collection.find()
            .limit(limit)
            .skip(skip)
            .toList()
    }

    suspend fun findById(id: String): Breed? {
        return collection.findOneById(id)
    }

    suspend fun findBySpeciesId(speciesId: String, limit: Int = 100, skip: Int = 0): List<Breed> {
        return collection.find(Breed::speciesId eq speciesId)
            .limit(limit)
            .skip(skip)
            .toList()
    }

    suspend fun search(query: String, limit: Int = 100, skip: Int = 0): List<Breed> {
        val pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE)

        // Ampliamos la búsqueda para incluir más campos
        val filter = org.litote.kmongo.or(
            Breed::names.elemMatch(LocalizedText::text regex pattern),
            Breed::descriptions.elemMatch(LocalizedText::text regex pattern),
            Breed::characteristics / BreedCharacteristics::temperament `in` listOf(query),
            Breed::characteristics / BreedCharacteristics::coatTypes `in` listOf(query),
            Breed::characteristics / BreedCharacteristics::colors `in` listOf(query)
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
        speciesId: String? = null,
        size: String? = null,
        temperament: List<String>? = null,
        colors: List<String>? = null,
        coatTypes: List<String>? = null,
        languages: List<String>? = null,
        minWeight: Double? = null,
        maxWeight: Double? = null,
        limit: Int = 100,
        skip: Int = 0
    ): List<Breed> {
        val filters = mutableListOf<Bson>()

        // Filtro de texto general
        query?.let {
            val pattern = Pattern.compile(it, Pattern.CASE_INSENSITIVE)
            filters.add(
                org.litote.kmongo.or(
                    Breed::names.elemMatch(LocalizedText::text regex pattern),
                    Breed::descriptions.elemMatch(LocalizedText::text regex pattern)
                )
            )
        }

        // Filtro por especie
        speciesId?.let {
            filters.add(Breed::speciesId eq it)
        }

        // Filtro por tamaño
        size?.let {
            filters.add(Breed::characteristics / BreedCharacteristics::size eq it)
        }

        // Filtro por temperamento
        temperament?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::characteristics / BreedCharacteristics::temperament `in` it)
            }
        }

        // Filtro por colores
        colors?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::characteristics / BreedCharacteristics::colors `in` it)
            }
        }

        // Filtro por tipo de pelaje
        coatTypes?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::characteristics / BreedCharacteristics::coatTypes `in` it)
            }
        }

        // Filtro por idioma disponible
        languages?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::names.elemMatch(LocalizedText::language `in` it))
            }
        }

        // Filtro por rango de peso
        if (minWeight != null || maxWeight != null) {
            minWeight?.let {
                filters.add(gt("characteristics.weightRange.min", it))
            }

            maxWeight?.let {
                filters.add(lt("characteristics.weightRange.max", it))
            }
        }

        if (filters.isEmpty()) {
            EMPTY_BSON
        } else {
            org.litote.kmongo.and(*filters.toTypedArray())
        }

        return findAll(limit, skip)
    }

    suspend fun searchByCharacteristics(
        size: String? = null,
        temperament: String? = null,
        limit: Int = 100,
        skip: Int = 0
    ): List<Breed> {
        val filters = mutableListOf<org.bson.conversions.Bson>()

        size?.let {
            filters.add(Breed::characteristics / BreedCharacteristics::size eq it)
        }

        temperament?.let {
            filters.add(org.bson.Document("characteristics.temperament",
                org.bson.Document("\$regex", it).append("\$options", "i")))
        }

        val filter = if (filters.isEmpty()) {
            org.litote.kmongo.EMPTY_BSON
        } else {
            org.litote.kmongo.and(*filters.toTypedArray())
        }

        return collection.find(filter)
            .limit(limit)
            .skip(skip)
            .toList()
    }

    suspend fun insert(breed: Breed): String {
        collection.insertOne(breed)
        return breed.id
    }

    suspend fun update(id: String, breed: Breed): Boolean {
        return collection.updateOneById(id, breed).modifiedCount > 0
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

    suspend fun countBySpeciesId(speciesId: String): Long {
        return collection.countDocuments(Breed::speciesId eq speciesId)
    }

    suspend fun countAdvancedSearch(
        query: String? = null,
        speciesId: String? = null,
        size: String? = null,
        temperament: List<String>? = null,
        colors: List<String>? = null,
        coatTypes: List<String>? = null,
        languages: List<String>? = null,
        minWeight: Double? = null,
        maxWeight: Double? = null
    ): Long {
        val filters = mutableListOf<Bson>()

        query?.let {
            val pattern = Pattern.compile(it, Pattern.CASE_INSENSITIVE)
            filters.add(
                org.litote.kmongo.or(
                    Breed::names.elemMatch(LocalizedText::text regex pattern),
                    Breed::descriptions.elemMatch(LocalizedText::text regex pattern)
                )
            )
        }

        speciesId?.let {
            filters.add(Breed::speciesId eq it)
        }

        size?.let {
            filters.add(Breed::characteristics / BreedCharacteristics::size eq it)
        }

        temperament?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::characteristics / BreedCharacteristics::temperament `in` it)
            }
        }

        colors?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::characteristics / BreedCharacteristics::colors `in` it)
            }
        }

        coatTypes?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::characteristics / BreedCharacteristics::coatTypes `in` it)
            }
        }

        languages?.let {
            if (it.isNotEmpty()) {
                filters.add(Breed::names.elemMatch(LocalizedText::language `in` it))
            }
        }

        if (minWeight != null || maxWeight != null) {
            minWeight?.let {
                filters.add(gt("characteristics.weightRange.min", it))
            }

            maxWeight?.let {
                filters.add(lt("characteristics.weightRange.max", it))
            }
        }

        val finalFilter = if (filters.isEmpty()) {
            EMPTY_BSON
        } else {
            org.litote.kmongo.and(*filters.toTypedArray())
        }

        return count()
    }
}