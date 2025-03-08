package com.pethunt.server.services

import com.pethunt.server.models.User
import com.pethunt.server.models.UserCreateDTO
import com.pethunt.server.models.UserDTO
import com.pethunt.server.repositories.UserRepository
import io.ktor.server.auth.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

class UserService(private val repository: UserRepository) {

    suspend fun createUser(userDTO: UserCreateDTO): UserDTO {
        validateEmail(userDTO.email)
        validateUsername(userDTO.username)
        validatePassword(userDTO.password)

        repository.findByEmail(userDTO.email)?.let {
            throw IllegalArgumentException("El email ya está registrado")
        }

        repository.findByUsername(userDTO.username)?.let {
            throw IllegalArgumentException("El nombre de usuario ya está en uso")
        }

        val passwordHash = hashPassword(userDTO.password)

        return repository.create(userDTO, passwordHash)
    }

    suspend fun getUserById(id: UUID): UserDTO? {
        return repository.findById(id)
    }

    suspend fun getUserByEmail(email: String): UserDTO? {
        return repository.findByEmail(email)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun validateCredentials(credentials: UserPasswordCredential): UserIdPrincipal? {
        val user = repository.findByEmail(credentials.name) ?: return null
        val userId = user.id.toJavaUuid()

        if (!verifyPassword(credentials.password, userId)) {
            return null
        }

        repository.updateLastLogin(userId)

        return UserIdPrincipal(user.username)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun updateUser(id: UUID, userDTO: UserCreateDTO): UserDTO? {
        validateEmail(userDTO.email)
        validateUsername(userDTO.username)

        repository.findByEmail(userDTO.email)?.let {
            if (it.id.toJavaUuid() != id) {
                throw IllegalArgumentException("El email ya está registrado")
            }
        }

        repository.findByUsername(userDTO.username)?.let {
            if (it.id.toJavaUuid() != id) {
                throw IllegalArgumentException("El nombre de usuario ya está en uso")
            }
        }

        val updated = repository.update(id, userDTO)

        if (!updated) {
            return null
        }

        return repository.findById(id)
    }

    fun changePassword(id: UUID, currentPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(currentPassword, id)) {
            throw IllegalArgumentException("La contraseña actual es incorrecta")
        }

        validatePassword(newPassword)

        return transaction {
            val user = User.findById(id) ?: return@transaction false
            user.passwordHash = hashPassword(newPassword)
            true
        }
    }

    private fun hashPassword(password: String): String {
        return hashPassword(password)
    }

    private fun verifyPassword(password: String, userId: UUID): Boolean {
        val user = User.findById(userId) ?: return false

        val passwordHash = hashPassword(password)
        return passwordHash == user.passwordHash
    }

    private fun validateEmail(email: String) {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)$")
        if (!email.matches(emailRegex)) {
            throw IllegalArgumentException("Formato de email inválido")
        }
    }

    private fun validateUsername(username: String) {
        if (username.length < 3 || username.length > 50) {
            throw IllegalArgumentException("El nombre de usuario debe tener entre 3 y 50 caracteres")
        }

        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw IllegalArgumentException("El nombre de usuario solo puede contener letras, números, guiones y guiones bajos")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw IllegalArgumentException("La contraseña debe tener al menos 8 caracteres")
        }
    }
}