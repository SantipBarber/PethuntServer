package com.pethunt.server.services

import com.pethunt.server.models.PetCreateDTO
import com.pethunt.server.models.PetDTO
import com.pethunt.server.repositories.PetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

class PetServiceTest {
    private lateinit var petRepository: PetRepository
    private lateinit var petService: PetService

    @BeforeTest
    fun setup() {
        petRepository = mockk()
        petService = PetService(petRepository)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `createPet should validate data and create pet`() = runTest {
        // Given
        val userId = UUID.randomUUID()
        val petDTO = PetCreateDTO(
            name = "Max",
            speciesId = "dog",
            birthDate = "2020-01-01",
            gender = "MALE",
            weight = 25.5,
            bio = "Friendly dog"
        )
        val expectedPetDTO = PetDTO(
            id = UUID.randomUUID().toKotlinUuid(),
            userId = userId.toKotlinUuid(),
            name = petDTO.name,
            speciesId = petDTO.speciesId,
            birthDate = petDTO.birthDate,
            gender = petDTO.gender,
            weight = petDTO.weight,
            bio = petDTO.bio,
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z",
            isActive = true
        )

        coEvery { petRepository.create(userId, petDTO) } returns expectedPetDTO

        // When
        val result = petService.createPet(userId, petDTO)

        // Then
        coVerify { petRepository.create(userId, petDTO) }
        assertEquals(expectedPetDTO, result)
    }

    @Test
    fun `createPet should throw exception when name is blank`() = runTest {
        // Given
        val userId = UUID.randomUUID()
        val petDTO = PetCreateDTO(
            name = "",  // Nombre en blanco
            speciesId = "dog"
        )

        // Then
        val exception = assertFailsWith<IllegalArgumentException> {
            // When
            petService.createPet(userId, petDTO)
        }

        assertEquals("El nombre de la mascota no puede estar vacío", exception.message)
    }
}