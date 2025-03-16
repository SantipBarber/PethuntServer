package com.pethunt.server.repositories

import com.pethunt.server.models.Breed
import com.pethunt.server.models.BreedCharacteristics
import com.pethunt.server.models.LocalizedText
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import java.util.regex.Pattern.CASE_INSENSITIVE
import java.util.regex.Pattern.compile

class BreedRepository(private val database: CoroutineDatabase) {
    private val collection = database.getCollection<Breed>()
    
    suspend fun findAll(limit: Int = 100, skip: Int = 0): List<Breed> {
        return collection.find()
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun findById(id: String): Breed? {
        return collection.findOneById(id)
    }
    
    suspend fun findBySpeciesId(speciesId: String, limit: Int = 100, skip: Int = 0): List<Breed> {
        return collection.find(Breed::speciesId eq speciesId)
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun search(query: String, limit: Int = 100, skip: Int = 0): List<Breed> {
        val pattern = compile(query, CASE_INSENSITIVE)

        val filter = or(
            Breed::names.elemMatch(LocalizedText::text regex pattern),
            Breed::descriptions.elemMatch(LocalizedText::text regex pattern)
        )
        
        return collection.find(filter)
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun searchByCharacteristics(
        size: String? = null,
        temperament: String? = null,
        limit: Int = 100,
        skip: Int = 0
    ): List<Breed> {
        val filters = mutableListOf<org.bson.conversions.Bson>()
        
        size?.let {
            filters.add(Breed::characteristics / BreedCharacteristics::size eq it)
        }
        
        temperament?.let {
            filters.add(Breed::characteristics / BreedCharacteristics::temperament contains it)
        }
        
        val filter = if (filters.isEmpty()) {
            EMPTY_BSON
        } else {
            and(*filters.toTypedArray())
        }
        
        return collection.find(filter)
            .limit(limit)
            .skip(skip)
            .toList()
    }
    
    suspend fun insert(breed: Breed): String {
        collection.insertOne(breed)
        return breed.id
    }
    
    suspend fun update(id: String, breed: Breed): Boolean {
        return collection.updateOneById(id, breed).modifiedCount > 0
    }
    
    suspend fun delete(id: String): Boolean {
        return collection.deleteOneById(id).deletedCount > 0
    }
    
    suspend fun count(): Long {
        return collection.countDocuments()
    }
    
    suspend fun countBySpeciesId(speciesId: String): Long {
        return collection.countDocuments(Breed::speciesId eq speciesId)
    }
}