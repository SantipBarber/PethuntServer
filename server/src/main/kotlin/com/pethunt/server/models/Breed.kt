package com.pethunt.server.models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class Breed(
    @BsonId
    val id: String = ObjectId().toString(),
    val speciesId: String,
    val names: List<LocalizedText> = emptyList(),
    val descriptions: List<LocalizedText> = emptyList(),
    val characteristics: BreedCharacteristics? = null,
    val breedSpecificInfo: BreedInfo? = null,
    val statistics: BreedStatistics? = null,
    val media: MediaInfo? = null,
    val metadata: Metadata? = null
)

@Serializable
data class BreedCharacteristics(
    val size: String,
    val weightRange: Range? = null,
    val heightRange: Range? = null,
    val coatTypes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val temperament: List<String> = emptyList()
)

@Serializable
data class Range(
    val min: Double,
    val max: Double,
    val unit: String
)

@Serializable
data class BreedInfo(
    val origin: String = "",
    val purpose: List<String> = emptyList(),
    val popularity: Int? = null,  // Ranking
    val recognizedBy: List<String> = emptyList()  // Organizaciones que reconocen la raza
)

@Serializable
data class BreedStatistics(
    val averageLifespan: Double? = null,
    val populationEstimate: Int? = null,
    val registrationsPerYear: Int? = null
)