package com.pethunt.server.repositories

import com.pethunt.server.config.DatabaseFactory
import com.pethunt.server.models.User
import com.pethunt.server.models.UserDTO
import com.pethunt.server.models.Users
import com.pethunt.server.models.toUser
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UserRepository(private val dbFactory: DatabaseFactory) {

    @OptIn(ExperimentalUuidApi::class)
    suspend fun create(userDTO: UserDTO, passwordHash: String): User? = dbFactory.dbQuery {
        val userId = UUID.randomUUID()

        val insertStatement = Users.insert {
            it[id] = userId
            it[email] = userDTO.email
            it[username] = userDTO.username
            it[Users.passwordHash] = passwordHash
            it[fullName] = userDTO.fullName
            it[city] = userDTO.city
            it[region] = userDTO.region
            it[country] = userDTO.country
            // createdAt y updatedAt utilizan los valores por defecto de la tabla
        }

        insertStatement.resultedValues?.singleOrNull()?.toUser()
    }

    suspend fun findByEmail(email: String): User? = dbFactory.dbQuery {
        Users.select { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun findByUsername(username: String): User? = dbFactory.dbQuery {
        Users.select { Users.username eq username }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun findById(id: String): User? = dbFactory.dbQuery {
        val uuid = UUID.fromString(id)
        try {
            Users.select { Users.id eq id }
                .map { it.toUser() }
                .singleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun updateLastLogin(id: Uuid): Boolean = dbFactory.dbQuery {
        try {
            val uuid = id
            Users.update({ Users.id eq uuid }) {
                it[lastLogin] = Clock.System.now().toJavaInstant()
            } > 0
        } catch (e: Exception) {
            false
        }
    }
}