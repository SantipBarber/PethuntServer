package com.pethunt.server.repositories

import com.pethunt.server.models.User
import com.pethunt.server.models.UserCreateDTO
import com.pethunt.server.models.UserDTO
import com.pethunt.server.models.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserRepository {

    /**
     * Crea un nuevo usuario
     */
    suspend fun create(userCreateDTO: UserCreateDTO, passwordHash: String): UserDTO = newSuspendedTransaction(Dispatchers.IO) {
        User.new {
            email = userCreateDTO.email
            username = userCreateDTO.username
            this.passwordHash = passwordHash
            fullName = userCreateDTO.fullName
            city = userCreateDTO.city
            region = userCreateDTO.region
            country = userCreateDTO.country
        }.toDTO()
    }

    /**
     * Busca un usuario por email
     */
    suspend fun findByEmail(email: String): UserDTO? = newSuspendedTransaction(Dispatchers.IO) {
        User.find { UsersTable.email eq email }
            .singleOrNull()
            ?.toDTO()
    }

    /**
     * Busca un usuario por nombre de usuario
     */
    suspend fun findByUsername(username: String): UserDTO? = newSuspendedTransaction(Dispatchers.IO) {
        User.find { UsersTable.username eq username }
            .singleOrNull()
            ?.toDTO()
    }

    /**
     * Busca un usuario por ID
     */
    suspend fun findById(id: UUID): UserDTO? = newSuspendedTransaction(Dispatchers.IO) {
        User.findById(id)?.toDTO()
    }

    /**
     * Actualiza el último login de un usuario
     */
    suspend fun updateLastLogin(id: UUID): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val user = User.findById(id) ?: return@newSuspendedTransaction false
        user.lastLogin = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        true
    }

    /**
     * Actualiza los datos de un usuario
     */
    suspend fun update(id: UUID, userDTO: UserCreateDTO): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val user = User.findById(id) ?: return@newSuspendedTransaction false

        user.apply {
            username = userDTO.username
            email = userDTO.email
            fullName = userDTO.fullName
            city = userDTO.city
            region = userDTO.region
            country = userDTO.country
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        true
    }

    /**
     * Elimina un usuario
     */
    suspend fun delete(id: UUID): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val user = User.findById(id) ?: return@newSuspendedTransaction false
        user.delete()
        true
    }

    /**
     * Obtiene todos los usuarios con paginación
     */
    suspend fun getAllUsers(limit: Int = 100, offset: Long = 0): List<UserDTO> = newSuspendedTransaction(Dispatchers.IO) {
        User.all()
            .limit(limit).offset(offset)
            .map { it.toDTO() }
    }
}