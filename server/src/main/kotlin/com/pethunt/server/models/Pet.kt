package com.pethunt.server.models

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


object PetsTable : UUIDTable("user_pets") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 50)
    val speciesId = varchar("species_id", 100) // Guardamos el ID de MongoDB como string
    val breedId = varchar("breed_id", 100).nullable() // Algunas mascotas pueden no tener raza específica
    val birthDate = date("birth_date").nullable()
    val gender = varchar("gender", 20).nullable()
    val weight = double("weight").nullable()
    val bio = text("bio").nullable()
    val profileImageUrl = varchar("profile_image_url", 255).nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val isActive = bool("is_active").default(true)
}

@OptIn(ExperimentalUuidApi::class)
class Pet(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Pet>(PetsTable)

    var userId by PetsTable.userId
    var name by PetsTable.name
    var speciesId by PetsTable.speciesId
    var breedId by PetsTable.breedId
    var birthDate by PetsTable.birthDate
    var gender by PetsTable.gender
    var weight by PetsTable.weight
    var bio by PetsTable.bio
    var profileImageUrl by PetsTable.profileImageUrl
    var createdAt by PetsTable.createdAt
    var updatedAt by PetsTable.updatedAt
    var isActive by PetsTable.isActive

    // Relación con el usuario
    var user by User referencedOn PetsTable.userId

    fun toDTO(): PetDTO = PetDTO(
        id = id.value.toKotlinUuid(),
        userId = userId.value.toKotlinUuid(),
        name = name,
        speciesId = speciesId,
        breedId = breedId,
        birthDate = birthDate?.toString(),
        gender = gender,
        weight = weight,
        bio = bio,
        profileImageUrl = profileImageUrl,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        isActive = isActive
    )
}

@Serializable
data class PetDTO @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val userId: Uuid,
    val name: String,
    val speciesId: String,
    val breedId: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val weight: Double? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val isActive: Boolean = true
)

@Serializable
data class PetCreateDTO(
    val name: String,
    val speciesId: String,
    val breedId: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val weight: Double? = null,
    val bio: String? = null
)

enum class PetGender {
    MALE, FEMALE, UNKNOWN;

    companion object {
        fun fromString(value: String?): PetGender? {
            return when(value?.uppercase()) {
                "MALE" -> MALE
                "FEMALE" -> FEMALE
                "UNKNOWN" -> UNKNOWN
                else -> null
            }
        }
    }
}