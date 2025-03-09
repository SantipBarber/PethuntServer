package com.pethunt.server.services

import com.pethunt.server.models.PetCreateDTO
import com.pethunt.server.models.PetDTO
import com.pethunt.server.repositories.PetRepository
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

class PetService(private val repository: PetRepository) {

    suspend fun createPet(userId: UUID, petDTO: PetCreateDTO): PetDTO {
        validatePetData(petDTO)
        return repository.create(userId, petDTO)
    }

    suspend fun getPetById(id: UUID): PetDTO? {
        return repository.findById(id)
    }

    suspend fun getPetsByUserId(userId: UUID, limit: Int = 100, offset: Long = 0): List<PetDTO> {
        return repository.findByUserId(userId, limit, offset)
    }

    suspend fun updatePet(id: UUID, petDTO: PetCreateDTO): PetDTO? {
        validatePetData(petDTO)
        
        val updated = repository.update(id, petDTO)
        
        if (!updated) {
            return null
        }
        
        return repository.findById(id)
    }

    suspend fun updatePetProfileImage(id: UUID, imageUrl: String): Boolean {
        if (imageUrl.isBlank()) {
            throw IllegalArgumentException("La URL de la imagen no puede estar vacía")
        }
        
        return repository.updateProfileImage(id, imageUrl)
    }

    suspend fun deletePet(id: UUID): Boolean {
        return repository.delete(id)
    }


    suspend fun hardDeletePet(id: UUID): Boolean {
        return repository.hardDelete(id)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun isOwner(petId: UUID, userId: UUID): Boolean {
        val pet = repository.findById(petId) ?: return false
        return pet.userId.toJavaUuid() == userId
    }

    private fun validatePetData(petDTO: PetCreateDTO) {
        if (petDTO.name.isBlank()) {
            throw IllegalArgumentException("El nombre de la mascota no puede estar vacío")
        }
        
        if (petDTO.name.length < 2 || petDTO.name.length > 50) {
            throw IllegalArgumentException("El nombre de la mascota debe tener entre 2 y 50 caracteres")
        }
        
        if (petDTO.speciesId.isBlank()) {
            throw IllegalArgumentException("La especie de la mascota es obligatoria")
        }
        
        // Validar formato de fecha si se proporciona
        petDTO.birthDate?.let {
            try {
                java.time.LocalDate.parse(it)
            } catch (e: Exception) {
                throw IllegalArgumentException("El formato de fecha de nacimiento debe ser YYYY-MM-DD")
            }
        }
        
        // Validar peso si se proporciona
        petDTO.weight?.let {
            if (it <= 0) {
                throw IllegalArgumentException("El peso debe ser un valor positivo")
            }
        }
    }

    suspend fun countPetsByUserId(userId: UUID): Long {
        return repository.countByUserId(userId)
    }
}