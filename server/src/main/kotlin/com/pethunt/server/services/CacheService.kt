package com.pethunt.server.services

import com.pethunt.server.config.RedisFactory
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import java.util.concurrent.TimeUnit

class CacheService(val redisFactory: RedisFactory) {
    val logger = LoggerFactory.getLogger(this::class.java)
    val json = Json { ignoreUnknownKeys = true }

    companion object {
        // TTLs como constantes públicas
        val DEFAULT_TTL = TimeUnit.MINUTES.toSeconds(30) // 30 minutes
        val SPECIES_TTL = TimeUnit.HOURS.toSeconds(24) // 24 hours
        val BREED_TTL = TimeUnit.HOURS.toSeconds(24) // 24 hours
        val SEARCH_RESULTS_TTL = TimeUnit.MINUTES.toSeconds(10) // 10 minutes
    }

    inline fun <T> useJedis(block: (Jedis) -> T): T {
        return redisFactory.jedisPool.resource.use(block)
    }

    /**
     * Get a value from cache
     */
    inline fun <reified T> get(key: String): T? {
        return try {
            useJedis { jedis ->
                val value = jedis.get(key) ?: return@useJedis null
                json.decodeFromString<T>(value)
            }
        } catch (e: Exception) {
            logger.error("Error getting from cache: $key", e)
            null
        }
    }

    /**
     * Set a value in cache with default TTL
     */
    inline fun <reified T> set(key: String, value: T, ttlSeconds: Long = DEFAULT_TTL) {
        try {
            val serialized = json.encodeToString(value)
            useJedis { jedis ->
                jedis.setex(key, ttlSeconds, serialized)
            }
        } catch (e: Exception) {
            logger.error("Error setting cache: $key", e)
        }
    }

    /**
     * Set a value in cache with specific TTL based on type
     */
    inline fun <reified T> setWithTypeTtl(key: String, value: T) {
        val ttl = when {
            key.startsWith("species:") -> SPECIES_TTL
            key.startsWith("breed:") -> BREED_TTL
            key.startsWith("search:") -> SEARCH_RESULTS_TTL
            else -> DEFAULT_TTL
        }
        set(key, value, ttl)
    }

    /**
     * Delete a specific key from cache
     */
    fun delete(key: String) {
        try {
            useJedis { jedis ->
                jedis.del(key)
            }
        } catch (e: Exception) {
            logger.error("Error deleting from cache: $key", e)
        }
    }

    /**
     * Delete multiple keys matching a pattern
     */
    fun deletePattern(pattern: String) {
        try {
            useJedis { jedis ->
                val keys = jedis.keys(pattern)
                if (keys.isNotEmpty()) {
                    jedis.del(*keys.toTypedArray())
                }
            }
        } catch (e: Exception) {
            logger.error("Error deleting pattern from cache: $pattern", e)
        }
    }

    /**
     * Check if a key exists in cache
     */
    fun exists(key: String): Boolean {
        return try {
            useJedis { jedis ->
                jedis.exists(key)
            }
        } catch (e: Exception) {
            logger.error("Error checking existence in cache: $key", e)
            false
        }
    }

    /**
     * Clear entire cache (use with caution)
     */
    fun flushAll() {
        try {
            useJedis { jedis ->
                jedis.flushAll()
            }
        } catch (e: Exception) {
            logger.error("Error flushing cache", e)
        }
    }
}