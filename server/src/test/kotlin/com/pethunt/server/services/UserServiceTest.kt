package com.pethunt.server.services

import com.pethunt.server.models.UserCreateDTO
import com.pethunt.server.models.UserDTO
import com.pethunt.server.repositories.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeTest
    fun setup() {
        userRepository = mockk()
        userService = UserService(userRepository)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `createUser should hash password and save user`() = runTest {
        // Given
        val userDTO = UserCreateDTO(
            email = "test@example.com",
            password = "password123",
            username = "testuser"
        )
        val expectedUserId = UUID.randomUUID()
        val hashedPassword = slot<String>()

        coEvery { userRepository.findByEmail(any()) } returns null
        coEvery { userRepository.findByUsername(any()) } returns null
        coEvery { userRepository.create(any(), capture(hashedPassword)) } returns UserDTO(
            id = expectedUserId.toKotlinUuid(),
            email = userDTO.email,
            username = userDTO.username,
            createdAt = System.currentTimeMillis()
        )

        // When
        val result = userService.createUser(userDTO)

        // Then
        coVerify { userRepository.create(userDTO, any()) }
        assertTrue(hashedPassword.captured.startsWith("\$2a\$") || hashedPassword.captured.startsWith("\$2y\$")) // BCrypt hash
        assertEquals(userDTO.email, result.email)
        assertEquals(userDTO.username, result.username)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `createUser should throw exception when email already exists`() = runTest {
        // Given
        val userDTO = UserCreateDTO(
            email = "existing@example.com",
            password = "password123",
            username = "newuser"
        )
        val existingUser = UserDTO(
            id = UUID.randomUUID().toKotlinUuid(),
            email = userDTO.email,
            username = "existinguser",
            createdAt = System.currentTimeMillis()
        )

        coEvery { userRepository.findByEmail(userDTO.email) } returns existingUser

        // Then
        val exception = assertFailsWith<IllegalArgumentException> {
            // When
            userService.createUser(userDTO)
        }

        assertEquals("El email ya está registrado", exception.message)
    }
}