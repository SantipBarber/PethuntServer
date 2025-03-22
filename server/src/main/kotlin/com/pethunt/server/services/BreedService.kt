package com.pethunt.server.services

import com.pethunt.server.models.Breed
import com.pethunt.server.models.PetsTable.speciesId
import com.pethunt.server.repositories.BreedRepository
import com.pethunt.server.repositories.SpeciesRepository
import com.pethunt.server.utils.PaginatedResponse
import com.pethunt.server.utils.PaginationUtils
import com.pethunt.server.utils.ValidationUtils

class BreedService(
    private val repository: BreedRepository,
    private val speciesRepository: SpeciesRepository,
    private val cacheService: CacheService
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
        val cacheKey = "breed:$id"
        cacheService.get<Breed>(cacheKey)?.let { return it }

        val breed = repository.findById(id)
        breed?.let { cacheService.setWithTypeTtl(cacheKey, it) }
        return breed
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
        val cacheKey = "search:breeds:bySpecies:$speciesId:page:${paginationParams.page}:size:${paginationParams.pageSize}"
        cacheService.get<PaginatedResponse<Breed>>(cacheKey)?.let { return it }

        val offset = PaginationUtils.calculateOffset(paginationParams)
        val breeds = repository.findBySpeciesId(speciesId.toString(), paginationParams.pageSize, offset.toInt())
        val total = repository.countBySpeciesId(speciesId.toString())

        val response = PaginationUtils.createPaginatedResponse(
            items = breeds,
            params = paginationParams,
            totalItems = total
        )

        cacheService.setWithTypeTtl(cacheKey, response)
        return response
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

        val id = repository.insert(breed)
        val createdBreed = repository.findById(id) ?: throw IllegalStateException("Error al crear la raza")

        cacheService.deletePattern("search:breeds*")

        return createdBreed
    }

    suspend fun updateBreed(id: String, breed: Breed): Breed? {
        val errors = ValidationUtils.validateBreed(breed)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString(", "))
        }

        val updated = repository.update(id, breed)
        if (!updated) {
            return null
        }

        cacheService.delete("breed:$id")
        cacheService.deletePattern("search:breeds*")

        return repository.findById(id)
    }

    suspend fun deleteBreed(id: String): Boolean {
        val deleted = repository.delete(id)
        if (deleted) {
            cacheService.delete("breed:$id")
            cacheService.deletePattern("search:breeds*")
        }
        return deleted
    }
}