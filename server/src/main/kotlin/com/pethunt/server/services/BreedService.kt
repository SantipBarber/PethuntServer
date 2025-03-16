package com.pethunt.server.services

import com.pethunt.server.models.Breed
import com.pethunt.server.repositories.BreedRepository
import com.pethunt.server.repositories.SpeciesRepository
import kotlin.math.min

class BreedService(
    private val repository: BreedRepository,
    private val speciesRepository: SpeciesRepository
) {
    
    suspend fun getAllBreeds(page: Int = 1, pageSize: Int = 20): BreedPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val breeds = repository.findAll(limit, skip)
        val total = repository.count()
        
        return BreedPage(
            items = breeds,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun getBreedById(id: String): Breed? {
        return repository.findById(id)
    }
    
    suspend fun getBreedsBySpeciesId(speciesId: String, page: Int = 1, pageSize: Int = 20): BreedPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val breeds = repository.findBySpeciesId(speciesId, limit, skip)
        val total = repository.countBySpeciesId(speciesId)
        
        return BreedPage(
            items = breeds,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun searchBreeds(query: String, page: Int = 1, pageSize: Int = 20): BreedPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val breeds = repository.search(query, limit, skip)
        val total = repository.count() // Idealmente deberíamos contar solo los resultados de la búsqueda
        
        return BreedPage(
            items = breeds,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun searchBreedsByCharacteristics(
        size: String? = null,
        temperament: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): BreedPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val breeds = repository.searchByCharacteristics(size, temperament, limit, skip)
        val total = repository.count() // Idealmente deberíamos contar solo los resultados de la búsqueda
        
        return BreedPage(
            items = breeds,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun createBreed(breed: Breed): Breed {
        validateBreed(breed)
        
        val id = repository.insert(breed)
        return repository.findById(id) ?: throw IllegalStateException("Error al crear la raza")
    }
    
    suspend fun updateBreed(id: String, breed: Breed): Breed? {
        validateBreed(breed)
        
        val updated = repository.update(id, breed)
        if (!updated) {
            return null
        }
        
        return repository.findById(id)
    }
    
    suspend fun deleteBreed(id: String): Boolean {
        return repository.delete(id)
    }
    
    private suspend fun validateBreed(breed: Breed) {
        if (breed.speciesId.isBlank()) {
            throw IllegalArgumentException("El ID de la especie no puede estar vacío")
        }
        
        // Verificar que la especie existe
        val species = speciesRepository.findById(breed.speciesId)
            ?: throw IllegalArgumentException("La especie no existe")
        
        if (breed.names.isEmpty()) {
            throw IllegalArgumentException("Debe proporcionar al menos un nombre para la raza")
        }
    }
}

data class BreedPage(
    val items: List<Breed>,
    val pagination: Pagination
)