package com.pethunt.server.services

import com.pethunt.server.models.Breed
import com.pethunt.server.repositories.BreedRepository
import com.pethunt.server.repositories.SpeciesRepository
import com.pethunt.server.utils.PaginatedResponse
import com.pethunt.server.utils.PaginationUtils
import com.pethunt.server.utils.ValidationUtils

class BreedService(
    private val repository: BreedRepository,
    private val speciesRepository: SpeciesRepository
) {

    suspend fun getAllBreeds(
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Breed> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val breeds = repository.findAll(paginationParams.pageSize, offset.toInt())
        val total = repository.count()

        return PaginationUtils.createPaginatedResponse(
            items = breeds,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun getBreedById(id: String): Breed? {
        return repository.findById(id)
    }

    suspend fun getBreedsBySpeciesId(
        speciesId: String,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Breed> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val breeds = repository.findBySpeciesId(speciesId, paginationParams.pageSize, offset.toInt())
        val total = repository.countBySpeciesId(speciesId)

        return PaginationUtils.createPaginatedResponse(
            items = breeds,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun searchBreeds(
        query: String,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Breed> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val breeds = repository.search(query, paginationParams.pageSize, offset.toInt())
        val total = repository.count()

        return PaginationUtils.createPaginatedResponse(
            items = breeds,
            params = paginationParams,
            totalItems = total
        )
    }

    /**
     * Realiza una búsqueda avanzada utilizando múltiples criterios
     */
    suspend fun advancedSearchBreeds(
        query: String? = null,
        speciesId: String? = null,
        size: String? = null,
        temperament: List<String>? = null,
        colors: List<String>? = null,
        coatTypes: List<String>? = null,
        languages: List<String>? = null,
        minWeight: Double? = null,
        maxWeight: Double? = null,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Breed> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val breeds = repository.advancedSearch(
            query, speciesId, size, temperament, colors, coatTypes,
            languages, minWeight, maxWeight, paginationParams.pageSize, offset.toInt()
        )

        val total = repository.countAdvancedSearch(
            query, speciesId, size, temperament, colors, coatTypes,
            languages, minWeight, maxWeight
        )

        return PaginationUtils.createPaginatedResponse(
            items = breeds,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun searchBreedsByCharacteristics(
        size: String? = null,
        temperament: String? = null,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Breed> {
        val offset = PaginationUtils.calculateOffset(paginationParams)

        val breeds = repository.searchByCharacteristics(
            size, temperament, paginationParams.pageSize, offset.toInt()
        )

        val total = repository.count()

        return PaginationUtils.createPaginatedResponse(
            items = breeds,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun createBreed(breed: Breed): Breed {
        val errors = ValidationUtils.validateBreed(breed)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString(", "))
        }

        // Verificar que la especie existe
        speciesRepository.findById(breed.speciesId)
            ?: throw IllegalArgumentException("La especie no existe")

        val id = repository.insert(breed)
        return repository.findById(id) ?: throw IllegalStateException("Error al crear la raza")
    }

    suspend fun updateBreed(id: String, breed: Breed): Breed? {
        val errors = ValidationUtils.validateBreed(breed)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString(", "))
        }

        // Verificar que la especie existe
        speciesRepository.findById(breed.speciesId)
            ?: throw IllegalArgumentException("La especie no existe")

        val updated = repository.update(id, breed)
        if (!updated) {
            return null
        }

        return repository.findById(id)
    }

    suspend fun deleteBreed(id: String): Boolean {
        return repository.delete(id)
    }
}