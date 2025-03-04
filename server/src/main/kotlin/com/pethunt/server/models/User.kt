package com.pethunt.server.models

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/**
 * Definición de la tabla Users con Exposed
 */
object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val username = varchar("username", 50).uniqueIndex()
    val fullName = varchar("full_name", 100).nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val lastLogin = datetime("last_login").nullable()
    val isActive = bool("is_active").default(true)
    val role = varchar("role", 20).default("user")
    val locationPrivacy = varchar("location_privacy", 20).default("city_only")
    val city = varchar("city", 100).nullable()
    val region = varchar("region", 100).nullable()
    val country = varchar("country", 100).nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
}

/**
 * Entidad User usando Exposed DAO
 */
@OptIn(ExperimentalUuidApi::class)
class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(UsersTable)

    var email by UsersTable.email
    var passwordHash by UsersTable.passwordHash
    var username by UsersTable.username
    var fullName by UsersTable.fullName
    var createdAt by UsersTable.createdAt
    var updatedAt by UsersTable.updatedAt
    var lastLogin by UsersTable.lastLogin
    var isActive by UsersTable.isActive
    var role by UsersTable.role
    var locationPrivacy by UsersTable.locationPrivacy
    var city by UsersTable.city
    var region by UsersTable.region
    var country by UsersTable.country
    var latitude by UsersTable.latitude
    var longitude by UsersTable.longitude

    // Futuras relaciones:
    // val pets by Pet referrersOn PetsTable.userId

    fun toDTO(): UserDTO = UserDTO(
        id = id.value.toKotlinUuid(),
        email = email,
        username = username,
        fullName = fullName,
        role = role,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        isActive = isActive,
        city = city,
        region = region,
        country = country
    )
}

/**
 * DTO para transferencia de datos de usuario
 */
@Serializable
data class UserDTO @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val email: String,
    val username: String,
    val fullName: String? = null,
    val role: String = "user",
    val createdAt: Long,
    val isActive: Boolean = true,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null
)

/**
 * DTO para creación/actualización de usuarios
 */
@Serializable
data class UserCreateDTO(
    val email: String,
    val password: String,
    val username: String,
    val fullName: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null
)