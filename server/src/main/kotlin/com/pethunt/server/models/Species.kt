package com.pethunt.server.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class Species(
    @BsonId
    val id: String = ObjectId().toString(),
    val type: String,
    val scientificName: String,
    val commonNames: List<LocalizedText> = emptyList(),
    val descriptions: List<LocalizedText> = emptyList(),
    val characteristics: SpeciesCharacteristics? = null,
    val careInfo: CareInfo? = null,
    val media: MediaInfo? = null,
    val metadata: Metadata? = null
)

@Serializable
data class LocalizedText(
    val language: String,
    val text: String
)

@Serializable
data class SpeciesCharacteristics(
    val size: String,
    val lifespanMin: Int,
    val lifespanMax: Int,
    val weightMin: Double,
    val weightMax: Double,
    val temperament: List<String> = emptyList(),
    val habitat: List<String> = emptyList(),
    val diet: List<String> = emptyList()
)

@Serializable
data class CareInfo(
    val exerciseNeeds: Int,    // 1-5
    val groomingNeeds: Int,    // 1-5
    val trainingDifficulty: Int, // 1-5
    val healthIssues: List<String> = emptyList(),
    val dietaryRequirements: List<String> = emptyList()
)

@Serializable
data class MediaInfo(
    val mainImage: String = "",
    val gallery: List<String> = emptyList(),
    val videos: List<String> = emptyList()
)

@Serializable
data class Metadata(
    val source: String = "",
    val externalId: String = "",
    val lastUpdated: Long? = null,
    val verified: Boolean = false
)