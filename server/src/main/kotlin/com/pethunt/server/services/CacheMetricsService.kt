package com.pethunt.server.services

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

class CacheMetricsService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    // Contadores para las métricas
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val keyMetrics = ConcurrentHashMap<String, KeyMetrics>()
    
    // Métricas por patrón de clave
    data class KeyMetrics(
        val pattern: String,
        val hits: AtomicLong = AtomicLong(0),
        val misses: AtomicLong = AtomicLong(0)
    ) {
        val total: Long
            get() = hits.get() + misses.get()
            
        val hitRatio: Double
            get() = if (total > 0) hits.get().toDouble() / total else 0.0
    }
    
    fun recordHit(key: String) {
        hits.incrementAndGet()
        getOrCreateMetrics(key).hits.incrementAndGet()
    }
    
    fun recordMiss(key: String) {
        misses.incrementAndGet()
        getOrCreateMetrics(key).misses.incrementAndGet()
    }
    
    private fun getOrCreateMetrics(key: String): KeyMetrics {
        val pattern = when {
            key.startsWith("species:") -> "species:*"
            key.startsWith("breed:") -> "breed:*"
            key.startsWith("search:species:") -> "search:species:*"
            key.startsWith("search:breeds:") -> "search:breeds:*"
            else -> "other"
        }
        
        return keyMetrics.computeIfAbsent(pattern) { KeyMetrics(it) }
    }
    
    fun getHitRatio(): Double {
        val totalRequests = hits.get() + misses.get()
        return if (totalRequests > 0) hits.get().toDouble() / totalRequests else 0.0
    }
    
    fun getMetrics(): Map<String, Any> {
        val metrics = mutableMapOf<String, Any>()
        
        metrics["totalHits"] = hits.get()
        metrics["totalMisses"] = misses.get()
        metrics["hitRatio"] = getHitRatio()
        
        val patternMetrics = keyMetrics.values.map { metrics ->
            mapOf(
                "pattern" to metrics.pattern,
                "hits" to metrics.hits.get(),
                "misses" to metrics.misses.get(),
                "hitRatio" to metrics.hitRatio
            )
        }
        
        metrics["patterns"] = patternMetrics
        
        return metrics
    }
    
    fun logMetrics() {
        logger.info("Cache metrics - Hit ratio: ${String.format("%.2f", getHitRatio() * 100)}%")
        keyMetrics.values.forEach { metrics ->
            logger.info("Pattern ${metrics.pattern} - Hit ratio: ${String.format("%.2f", metrics.hitRatio * 100)}%")
        }
    }
    
    fun reset() {
        hits.set(0)
        misses.set(0)
        keyMetrics.clear()
    }
}