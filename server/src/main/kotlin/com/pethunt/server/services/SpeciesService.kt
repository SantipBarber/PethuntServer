package com.pethunt.server.services

import com.pethunt.server.models.Species
import com.pethunt.server.repositories.SpeciesRepository
import com.pethunt.server.utils.PaginatedResponse
import com.pethunt.server.utils.PaginationUtils
import com.pethunt.server.utils.ValidationUtils

class SpeciesService(
    private val repository: SpeciesRepository,
    private val cacheService: CacheService
) {

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
        val cacheKey = "species:$id"
        cacheService.get<Species>(cacheKey)?.let { return it }
        val species = repository.findById(id)
        species?.let { cacheService.setWithTypeTtl(cacheKey, it) }
        return species
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
        val total = repository.count()

        return PaginationUtils.createPaginatedResponse(
            items = species,
            params = paginationParams,
            totalItems = total
        )
    }

    suspend fun advancedSearchSpecies(
        query: String? = null,
        type: String? = null,
        temperament: List<String>? = null,
        size: String? = null,
        dietTypes: List<String>? = null,
        languages: List<String>? = null,
        paginationParams: PaginationUtils.PaginationParams
    ): PaginatedResponse<Species> {
        // Crear clave de caché basada en todos los parámetros
        val cacheKey = buildString {
            append("search:species:advanced:")
            append("q=${query ?: ""}")
            append(":type=${type ?: ""}")
            append(":temp=${temperament?.joinToString(",") ?: ""}")
            append(":size=${size ?: ""}")
            append(":diet=${dietTypes?.joinToString(",") ?: ""}")
            append(":lang=${languages?.joinToString(",") ?: ""}")
            append(":page=${paginationParams.page}")
            append(":size=${paginationParams.pageSize}")
        }

        // Intentar obtener de caché
        cacheService.get<PaginatedResponse<Species>>(cacheKey)?.let { return it }

        val offset = PaginationUtils.calculateOffset(paginationParams)

        val species = repository.advancedSearch(
            query, type, temperament, size, dietTypes, languages,
            paginationParams.pageSize, offset.toInt()
        )

        val total = repository.countAdvancedSearch(
            query, type, temperament, size, dietTypes, languages
        )

        val response = PaginationUtils.createPaginatedResponse(
            items = species,
            params = paginationParams,
            totalItems = total
        )

        // Guardar en caché
        cacheService.set(cacheKey, response, CacheService.SEARCH_RESULTS_TTL)

        return response
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
        val result = repository.delete(id)
        if(result) {
            cacheService.delete("species:$id")
            // invalidar posibles resultados de búsqueda
            cacheService.delete("search:species*")
        }
        return result
    }
}