package com.pethunt.server.services

import com.pethunt.server.models.User
import com.pethunt.server.models.UserDTO
import com.pethunt.server.repositories.UserRepository
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA512
import org.slf4j.LoggerFactory

class UserService(private val repository: UserRepository) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val hasher = CryptographyProvider.Default.get(SHA512).hasher()

    suspend fun createUser(userDTO: UserDTO): User? {
        val existingEmail = repository.findByEmail(userDTO.email)
        if (existingEmail != null) {
            throw IllegalArgumentException("Email already registered")
        }

        val existingUsername = repository.findByUsername(userDTO.username)
        if (existingUsername != null) {
            throw IllegalArgumentException("Username already taken")
        }

        val passwordHash = hashPassword(userDTO.password)
        return repository.create(userDTO, passwordHash)
    }

    suspend fun getUserByEmail(email: String): User? {
        return repository.findByEmail(email)
    }

    suspend fun getUserById(id: String): User? {
        return repository.findById(id)
    }

    suspend fun validateCredentials(email: String, password: String): User? {
        val user = repository.findByEmail(email) ?: return null

        try {
            // Obtener el hash de contraseña de la base de datos
            val userWithHash = getUserWithHash(email) ?: return null

            if (verifyPassword(password, userWithHash.second)) {
                repository.updateLastLogin(user.id.toString())
                return user
            }
        } catch (e: Exception) {
            logger.error("Error validating credentials", e)
        }

        return null
    }

    private suspend fun getUserWithHash(email: String): Pair<User, String>? {
        // Esta función debería obtener el usuario con su hash de contraseña
        // En una implementación real, obtendríamos los datos directamente de la base de datos
        val user = repository.findByEmail(email) ?: return null

        // Aquí estamos simulando la obtención del hash de la base de datos
        // En una implementación real, esto sería parte de la consulta principal
        // y no requeriría una consulta separada
        val passwordHash = "hash_simulado"  // simulación

        return Pair(user, passwordHash)
    }

    private suspend fun hashPassword(password: String): String {
        val hashBytes = hasher.hash(password.encodeToByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun verifyPassword(password: String, hash: String): Boolean {
        val passwordHash = hashPassword(password)
        return passwordHash == hash
    }
}