package com.pethunt.server.services

import com.pethunt.server.models.Species
import com.pethunt.server.repositories.SpeciesRepository
import com.pethunt.server.utils.PaginatedResponse
import com.pethunt.server.utils.PaginationUtils
import com.pethunt.server.utils.ValidationUtils

class SpeciesService(private val repository: SpeciesRepository) {

    suspend fun getAllSpecies(paginationParams: PaginationUtils.PaginationParams): PaginatedResponse<Species> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val species = repository.findAll(paginationParams.pageSize, offset.toInt())
        val total = repository.count()

        return PaginationUtils.createPaginatedResponse(
            items = species,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun getSpeciesById(id: String): Species? {
        return repository.findById(id)
    }

    suspend fun getSpeciesByType(
        type: String,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Species> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val species = repository.findByType(type, paginationParams.pageSize, offset.toInt())
        val total = repository.countByType(type)

        return PaginationUtils.createPaginatedResponse(
            items = species,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun searchSpecies(
        query: String,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Species> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val species = repository.search(query, paginationParams.pageSize, offset.toInt())
        // Idealmente deberíamos contar solo los resultados de la búsqueda
        val total = repository.count()

        return PaginationUtils.createPaginatedResponse(
            items = species,
            params = paginationParams,
            totalItems = total
        )
    }

    /**
     * Realiza una búsqueda avanzada utilizando múltiples criterios
     */
    suspend fun advancedSearchSpecies(
        query: String? = null,
        type: String? = null,
        temperament: List<String>? = null,
        size: String? = null,
        dietTypes: List<String>? = null,
        languages: List<String>? = null,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Species> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val species = repository.advancedSearch(
            query, type, temperament, size, dietTypes, languages,
            paginationParams.pageSize, offset.toInt()
        )

        val total = repository.countAdvancedSearch(
            query, type, temperament, size, dietTypes, languages
        )

        return PaginationUtils.createPaginatedResponse(
            items = species,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun createSpecies(species: Species): Species {
        val errors = ValidationUtils.validateSpecies(species)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString(", "))
        }

        val id = repository.insert(species)
        return repository.findById(id) ?: throw IllegalStateException("Error al crear la especie")
    }

    suspend fun updateSpecies(id: String, species: Species): Species? {
        val errors = ValidationUtils.validateSpecies(species)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString(", "))
        }

        val updated = repository.update(id, species)
        if (!updated) {
            return null
        }

        return repository.findById(id)
    }

    suspend fun deleteSpecies(id: String): Boolean {
        return repository.delete(id)
    }
}