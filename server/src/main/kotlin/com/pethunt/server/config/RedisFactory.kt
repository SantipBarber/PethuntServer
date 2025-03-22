package com.pethunt.server.config

import io.ktor.server.config.*
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol

class RedisFactory(private val config: ApplicationConfig) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    val jedisPool: JedisPool by lazy {
        try {
            val host = config.property("database.redis.host").getString()
            val port = config.property("database.redis.port").getString().toInt()
            val password = config.propertyOrNull("database.redis.password")?.getString()
            val database = config.propertyOrNull("database.redis.database")?.getString()?.toInt() ?: 0
            val timeout = config.propertyOrNull("database.redis.timeout")?.getString()?.toInt() ?: Protocol.DEFAULT_TIMEOUT
            
            logger.info("Initializing Redis connection to $host:$port DB:$database")
            
            val poolConfig = JedisPoolConfig().apply {
                maxTotal = 10
                maxIdle = 5
                minIdle = 1
                testOnBorrow = true
                testOnReturn = true
                testWhileIdle = true
            }
            
            if (password.isNullOrBlank()) {
                JedisPool(poolConfig, host, port, timeout, null, database)
            } else {
                JedisPool(poolConfig, host, port, timeout, password, database)
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Redis connection: ${e.message}", e)
            throw e
        }
    }
    
    fun testConnection(): Boolean {
        return try {
            jedisPool.resource.use { jedis ->
                jedis.ping() == "PONG"
            }
        } catch (e: Exception) {
            logger.error("Redis connection test failed", e)
            false
        }
    }
}