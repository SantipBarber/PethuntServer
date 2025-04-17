package com.pethunt.server.repositories

import com.pethunt.server.models.*
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class PetRepositoryTest {
    private lateinit var database: Database
    private lateinit var userRepository: UserRepository
    private lateinit var petRepository: PetRepository
    private lateinit var testUserId: UUID


    @BeforeTest
    fun setup() {
        try {
            // Crear una nueva base de datos con nombre único para cada prueba
            val timestamp = System.currentTimeMillis()
            database = Database.connect(
                url = "jdbc:h2:mem:test_${timestamp};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver"
            )

            // Crear tablas en el orden correcto
            transaction(database) {
                SchemaUtils.create(UsersTable)
                SchemaUtils.create(PetsTable)
            }

            userRepository = UserRepository()
            petRepository = PetRepository()

            // Crear un usuario de prueba
            runTest {
                val userDTO = userRepository.create(
                    UserCreateDTO(
                        email = "test@example.com",
                        password = "password123",
                        username = "testuser"
                    ),
                    "hashedpassword"
                )
                testUserId = UUID.fromString(userDTO.id.toString())
            }
        } catch (e: Exception) {
            println("Error al inicializar base de datos de prueba: ${e.message}")
            database = Database.connect("jdbc:h2:mem:dummy", driver = "org.h2.Driver")
            throw e
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            // Eliminar tablas en el orden correcto
            transaction(database) {
                SchemaUtils.drop(PetsTable)
                SchemaUtils.drop(UsersTable)
            }
        } catch (e: Exception) {
            println("Error durante la limpieza de la base de datos: ${e.message}")
        }
    }

    @Test
    fun `create should insert pet and return DTO`() = runTest {
        // Given
        val petCreateDTO = PetCreateDTO(
            name = "Max",
            speciesId = "dog",
            birthDate = "2020-01-01",
            gender = "MALE",
            weight = 25.5,
            bio = "Friendly dog"
        )

        // When
        val result = petRepository.create(testUserId, petCreateDTO)

        // Then
        assertEquals(petCreateDTO.name, result.name)
        assertEquals(petCreateDTO.speciesId, result.speciesId)
        assertEquals(petCreateDTO.birthDate, result.birthDate)

        // Verificar en base de datos
        transaction(database) {
            val pets = Pet.find { PetsTable.name eq petCreateDTO.name }.toList()
            assertEquals(1, pets.size)
            val pet = pets.first()
            assertEquals(testUserId, pet.userId.value)
            assertEquals(petCreateDTO.name, pet.name)
        }
    }

    @Test
    fun `findByUserId should return pets for user`() = runTest {
        // Given
        val petCreateDTO1 = PetCreateDTO(name = "Max", speciesId = "dog")
        val petCreateDTO2 = PetCreateDTO(name = "Bella", speciesId = "dog")

        petRepository.create(testUserId, petCreateDTO1)
        petRepository.create(testUserId, petCreateDTO2)

        // When
        val result = petRepository.findByUserId(testUserId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Max" })
        assertTrue(result.any { it.name == "Bella" })
    }

    @Test
    fun `update should modify pet properties`() = runTest {
        // Given
        val originalPet = PetCreateDTO(
            name = "Original",
            speciesId = "dog",
            weight = 10.0
        )

        val createdPet = petRepository.create(testUserId, originalPet)
        val petId = UUID.fromString(createdPet.id.toString())

        val updatedPet = PetCreateDTO(
            name = "Updated",
            speciesId = "dog",
            weight = 15.0
        )

        // When
        val result = petRepository.update(petId, updatedPet)

        // Then
        assertTrue(result)

        // Verify in database
        val retrievedPet = petRepository.findById(petId)
        assertNotNull(retrievedPet)
        assertEquals("Updated", retrievedPet.name)
        assertEquals(15.0, retrievedPet.weight)
    }

    @Test
    fun `delete should mark pet as inactive`() = runTest {
        // Given
        val pet = PetCreateDTO(name = "ToDelete", speciesId = "dog")
        val createdPet = petRepository.create(testUserId, pet)
        val petId = UUID.fromString(createdPet.id.toString())

        // When
        val result = petRepository.delete(petId)

        // Then
        assertTrue(result)

        // Verify pet is marked as inactive
        val retrievedPet = petRepository.findById(petId)
        assertNotNull(retrievedPet)
        assertFalse(retrievedPet.isActive)
    }

    @Test
    fun `hardDelete should remove pet from database`() = runTest {
        // Given
        val pet = PetCreateDTO(name = "ToHardDelete", speciesId = "dog")
        val createdPet = petRepository.create(testUserId, pet)
        val petId = UUID.fromString(createdPet.id.toString())

        // When
        val result = petRepository.hardDelete(petId)

        // Then
        assertTrue(result)

        // Verify pet is completely removed
        val retrievedPet = petRepository.findById(petId)
        assertNull(retrievedPet)
    }
}