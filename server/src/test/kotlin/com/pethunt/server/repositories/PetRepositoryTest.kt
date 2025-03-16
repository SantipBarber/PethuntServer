package com.pethunt.server.repositories

import com.pethunt.server.models.*
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

class PetRepositoryTest {
    private lateinit var database: Database
    private lateinit var userRepository: UserRepository
    private lateinit var petRepository: PetRepository
    private lateinit var testUserId: UUID
    
    @OptIn(ExperimentalUuidApi::class)
    @BeforeTest
    fun setup() {
        // Configurar base de datos H2 en memoria para tests
        database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        
        transaction(database) {
            SchemaUtils.create(UsersTable, PetsTable)
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
    }
    
    @AfterTest
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(PetsTable, UsersTable)
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
            val pet = Pet.find { PetsTable.name eq petCreateDTO.name }.singleOrNull()
            assertNotNull(pet)
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
}