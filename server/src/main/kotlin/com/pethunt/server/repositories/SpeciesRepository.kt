package com.pethunt.server.repositories

import com.pethunt.server.models.LocalizedText
import com.pethunt.server.models.Species
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.elemMatch
import org.litote.kmongo.eq
import org.litote.kmongo.or
import org.litote.kmongo.regex
import java.util.regex.Pattern

class SpeciesRepository(private val database: CoroutineDatabase) {
    private val collection = database.getCollection<Species>()
    
    suspend fun findAll(limit: Int = 100, skip: Int = 0): List<Species> {
        return collection.find()
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun findById(id: String): Species? {
        return collection.findOneById(id)
    }
    
    suspend fun findByType(type: String, limit: Int = 100, skip: Int = 0): List<Species> {
        return collection.find(Species::type eq type)
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun search(query: String, limit: Int = 100, skip: Int = 0): List<Species> {
        val pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE)

        val filter = or(
            Species::commonNames.elemMatch(LocalizedText::text regex pattern),
            Species::descriptions.elemMatch(LocalizedText::text regex pattern)
        )

        return collection.find(filter)
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun insert(species: Species): String {
        collection.insertOne(species)
        return species.id
    }
    
    suspend fun update(id: String, species: Species): Boolean {
        return collection.updateOneById(id, species).modifiedCount > 0
    }
    
    suspend fun delete(id: String): Boolean {
        return collection.deleteOneById(id).deletedCount > 0
    }
    
    suspend fun count(): Long {
        return collection.countDocuments()
    }
    
    suspend fun countByType(type: String): Long {
        return collection.countDocuments(Species::type eq type)
    }
}