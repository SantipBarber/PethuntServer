package com.pethunt.server.repositories

import com.pethunt.server.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class PetRepository {

    suspend fun create(userId: UUID, petCreateDTO: PetCreateDTO): PetDTO = newSuspendedTransaction(Dispatchers.IO) {
        Pet.new {
            this.userId = EntityID(userId, UsersTable)
            name = petCreateDTO.name
            speciesId = petCreateDTO.speciesId
            breedId = petCreateDTO.breedId
            birthDate = petCreateDTO.birthDate?.let {
                kotlinx.datetime.LocalDate.parse(input = it)
            }
            gender = petCreateDTO.gender
            weight = petCreateDTO.weight
            bio = petCreateDTO.bio
        }.toDTO()
    }

    suspend fun findById(id: UUID): PetDTO? = newSuspendedTransaction(Dispatchers.IO) {
        Pet.findById(id)?.toDTO()
    }

    suspend fun findByUserId(userId: UUID, limit: Int = 100, offset: Long = 0): List<PetDTO> = newSuspendedTransaction(Dispatchers.IO) {
        Pet.find { PetsTable.userId eq EntityID(userId, UsersTable) }
            .orderBy(PetsTable.createdAt to SortOrder.DESC)
            .limit(limit).offset(offset)
            .map { it.toDTO() }
    }

    suspend fun update(id: UUID, petDTO: PetCreateDTO): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val pet = Pet.findById(id) ?: return@newSuspendedTransaction false

        pet.apply {
            name = petDTO.name
            speciesId = petDTO.speciesId
            breedId = petDTO.breedId
            birthDate = petDTO.birthDate?.let {
                kotlinx.datetime.LocalDate.parse(input = it)
            }
            gender = petDTO.gender
            weight = petDTO.weight
            bio = petDTO.bio
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        true
    }

    suspend fun updateProfileImage(id: UUID, imageUrl: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val pet = Pet.findById(id) ?: return@newSuspendedTransaction false

        pet.apply {
            profileImageUrl = imageUrl
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        true
    }

    suspend fun delete(id: UUID): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val pet = Pet.findById(id) ?: return@newSuspendedTransaction false

        pet.apply {
            isActive = false
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        true
    }

    suspend fun hardDelete(id: UUID): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val pet = Pet.findById(id) ?: return@newSuspendedTransaction false
        pet.delete()
        true
    }

    suspend fun countByUserId(userId: UUID): Long = newSuspendedTransaction(Dispatchers.IO) {
        Pet.find { PetsTable.userId eq EntityID(userId, UsersTable) }.count()
    }
}