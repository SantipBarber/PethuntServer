package com.pethunt.server.repositories

import com.pethunt.server.models.User
import com.pethunt.server.models.UserCreateDTO
import com.pethunt.server.models.UsersTable
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class UserRepositoryTest {
    private lateinit var database: Database
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        try {
            // Crear una nueva base de datos con nombre único para cada prueba
            // usando un timestamp para garantizar unicidad
            val timestamp = System.currentTimeMillis()
            database = Database.connect(
                url = "jdbc:h2:mem:test_${timestamp};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver"
            )

            // Crear solo la tabla Users (sin PetsTable para evitar dependencias)
            transaction(database) {
                SchemaUtils.create(UsersTable)
            }

            userRepository = UserRepository()
        } catch (e: Exception) {
            println("Error al inicializar base de datos de prueba: ${e.message}")
            // Inicializar la base de datos con un valor predeterminado para evitar errores
            database = Database.connect("jdbc:h2:mem:dummy", driver = "org.h2.Driver")
            throw e // Volver a lanzar la excepción para que el test falle correctamente
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            // Eliminar solo la tabla Users
            transaction(database) {
                SchemaUtils.drop(UsersTable)
            }
        } catch (e: Exception) {
            // Ignorar errores en la limpieza
            println("Error durante la limpieza de la base de datos: ${e.message}")
        }
    }

    @Test
    fun `create should insert user and return DTO`() = runTest {
        // Given
        val userCreateDTO = UserCreateDTO(
            email = "test@example.com",
            password = "password123",
            username = "testuser"
        )
        val passwordHash = "hashedpassword"

        // When
        val result = userRepository.create(userCreateDTO, passwordHash)

        // Then
        assertEquals(userCreateDTO.email, result.email)
        assertEquals(userCreateDTO.username, result.username)

        // Verificar en base de datos
        transaction(database) {
            val user = User.find { UsersTable.email eq userCreateDTO.email }.singleOrNull()
            assertNotNull(user)
            assertEquals(passwordHash, user.passwordHash)
        }
    }

    @Test
    fun `findByEmail should return user when exists`() = runTest {
        // Given - Crear un usuario
        val userCreateDTO = UserCreateDTO(
            email = "test.find@example.com",
            password = "password123",
            username = "testfind"
        )
        val passwordHash = "hashedpassword"

        userRepository.create(userCreateDTO, passwordHash)

        // When
        val result = userRepository.findByEmail(userCreateDTO.email)

        // Then
        assertNotNull(result)
        assertEquals(userCreateDTO.email, result.email)
        assertEquals(userCreateDTO.username, result.username)
    }

    @Test
    fun `findByEmail should return null when user does not exist`() = runTest {
        // When
        val result = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertNull(result)
    }

    @Test
    fun `findByUsername should return user when exists`() = runTest {
        // Given - Crear un usuario
        val userCreateDTO = UserCreateDTO(
            email = "username.test@example.com",
            password = "password123",
            username = "findbyusername"
        )
        val passwordHash = "hashedpassword"

        userRepository.create(userCreateDTO, passwordHash)

        // When
        val result = userRepository.findByUsername(userCreateDTO.username)

        // Then
        assertNotNull(result)
        assertEquals(userCreateDTO.email, result.email)
        assertEquals(userCreateDTO.username, result.username)
    }
}