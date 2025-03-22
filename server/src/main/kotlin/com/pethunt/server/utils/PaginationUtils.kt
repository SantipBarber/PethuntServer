package com.pethunt.server.utils

import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.math.ceil
import kotlin.math.min

/**
 * Clase de utilidad para manejar paginación
 */
object PaginationUtils {
    // Configuración global por defecto
    private const val DEFAULT_PAGE = 1
    private const val DEFAULT_PAGE_SIZE = 20
    private const val MAX_PAGE_SIZE = 100
    
    /**
     * Extrae parámetros de paginación de la solicitud
     */
    fun extractPaginationParams(call: ApplicationCall): PaginationParams {
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_PAGE
        val requestedPageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_PAGE_SIZE
        val pageSize = min(requestedPageSize, MAX_PAGE_SIZE)
        
        return PaginationParams(page, pageSize)
    }
    
    /**
     * Calcula el offset para consultas paginadas
     */
    fun calculateOffset(params: PaginationParams): Long {
        return ((params.page - 1) * params.pageSize).toLong()
    }
    
    /**
     * Calcula el número total de páginas
     */
    fun calculateTotalPages(totalItems: Long, pageSize: Int): Long {
        return ceil(totalItems.toDouble() / pageSize).toLong()
    }
    
    /**
     * Genera información de enlaces para navegación entre páginas
     */
    fun generatePaginationLinks(baseUrl: String, params: PaginationParams, totalPages: Long): Map<String, String> {
        val links = mutableMapOf<String, String>()
        val urlBuilder = { page: Int -> "$baseUrl?page=$page&pageSize=${params.pageSize}" }
        
        // Primera página
        links["first"] = urlBuilder(1)
        
        // Última página
        links["last"] = urlBuilder(totalPages.toInt())
        
        // Página anterior
        if (params.page > 1) {
            links["prev"] = urlBuilder(params.page - 1)
        }
        
        // Página siguiente
        if (params.page < totalPages) {
            links["next"] = urlBuilder(params.page + 1)
        }
        
        return links
    }
    
    /**
     * Convierte una lista de cualquier tipo a un objeto paginado
     */
    fun <T> createPaginatedResponse(
        items: List<T>,
        params: PaginationParams,
        totalItems: Long,
        baseUrl: String? = null
    ): PaginatedResponse<T> {
        val totalPages = calculateTotalPages(totalItems, params.pageSize)
        
        val pagination = Pagination(
            total = totalItems,
            page = params.page,
            pageSize = params.pageSize,
            pages = totalPages
        )
        
        val links = baseUrl?.let { generatePaginationLinks(it, params, totalPages) }
        
        return PaginatedResponse(
            items = items,
            pagination = pagination,
            links = links
        )
    }
    
    /**
     * Clase para encapsular parámetros de paginación
     */
    data class PaginationParams(
        val page: Int,
        val pageSize: Int
    )
}

/**
 * Clase para encapsular información de paginación
 */
@Serializable
data class Pagination(
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val pages: Long
)

/**
 * Clase genérica para respuestas paginadas
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val pagination: Pagination,
    val links: Map<String, String>? = null
)