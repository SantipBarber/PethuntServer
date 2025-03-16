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
        // Configurar base de datos H2 en memoria para tests
        database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )

        transaction(database) {
            SchemaUtils.create(UsersTable)
        }

        userRepository = UserRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(UsersTable)
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
}