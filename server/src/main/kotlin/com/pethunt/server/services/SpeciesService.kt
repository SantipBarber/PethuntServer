package com.pethunt.server.services

import com.pethunt.server.models.Species
import com.pethunt.server.repositories.SpeciesRepository
import kotlinx.serialization.Serializable
import kotlin.math.min

class SpeciesService(private val repository: SpeciesRepository) {
    
    suspend fun getAllSpecies(page: Int = 1, pageSize: Int = 20): SpeciesPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val species = repository.findAll(limit, skip)
        val total = repository.count()
        
        return SpeciesPage(
            items = species,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun getSpeciesById(id: String): Species? {
        return repository.findById(id)
    }
    
    suspend fun getSpeciesByType(type: String, page: Int = 1, pageSize: Int = 20): SpeciesPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val species = repository.findByType(type, limit, skip)
        val total = repository.countByType(type)
        
        return SpeciesPage(
            items = species,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun searchSpecies(query: String, page: Int = 1, pageSize: Int = 20): SpeciesPage {
        val limit = min(pageSize, 100) // Máximo 100 por página
        val skip = (page - 1) * limit
        
        val species = repository.search(query, limit, skip)
        val total = repository.count() // Idealmente deberíamos contar solo los resultados de la búsqueda
        
        return SpeciesPage(
            items = species,
            pagination = Pagination(
                total = total,
                page = page,
                pageSize = limit,
                pages = (total + limit - 1) / limit
            )
        )
    }
    
    suspend fun createSpecies(species: Species): Species {
        validateSpecies(species)
        
        val id = repository.insert(species)
        return repository.findById(id) ?: throw IllegalStateException("Error al crear la especie")
    }
    
    suspend fun updateSpecies(id: String, species: Species): Species? {
        validateSpecies(species)
        
        val updated = repository.update(id, species)
        if (!updated) {
            return null
        }
        
        return repository.findById(id)
    }
    
    suspend fun deleteSpecies(id: String): Boolean {
        return repository.delete(id)
    }
    
    private fun validateSpecies(species: Species) {
        if (species.type.isBlank()) {
            throw IllegalArgumentException("El tipo de especie no puede estar vacío")
        }
        
        if (species.scientificName.isBlank()) {
            throw IllegalArgumentException("El nombre científico no puede estar vacío")
        }
        
        if (species.commonNames.isEmpty()) {
            throw IllegalArgumentException("Debe proporcionar al menos un nombre común")
        }
    }
}

@Serializable
data class SpeciesPage(
    val items: List<Species>,
    val pagination: Pagination
)

@Serializable
data class Pagination(
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val pages: Long
)