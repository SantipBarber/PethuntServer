package com.pethunt.server.services

import org.slf4j.LoggerFactory
import java.util.*

/**
 * Versión mock de StorageService para usar en pruebas
 * sin depender de Firebase
 */
class MockStorageService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val bucketName = "mock-bucket"
    private val storedImages = mutableMapOf<String, ByteArray>()
    
    /**
     * Simula subir una imagen y devuelve una URL mock
     */
    fun uploadImage(data: ByteArray, contentType: String, folder: String): String {
        val fileName = "${folder}/${UUID.randomUUID()}"
        val imagePath = "$bucketName/$fileName"
        
        // Almacenar los datos para verificaciones
        storedImages[imagePath] = data
        
        logger.info("Mock: Imagen guardada en $imagePath (${data.size} bytes)")
        
        // Devuelve una URL mock
        return "https://firebasestorage.example.com/v0/b/$imagePath?alt=media"
    }
    
    /**
     * Simula subir una imagen desde URL
     */
    fun uploadImageFromUrl(imageUrl: String, folder: String): String {
        // Simular datos de imagen
        val mockImageData = "Mock image data from URL: $imageUrl".toByteArray()
        return uploadImage(mockImageData, "image/jpeg", folder)
    }
    
    /**
     * Simula eliminar una imagen
     */
    fun deleteImage(imageUrl: String): Boolean {
        // Extraer el path de la URL
        val path = imageUrl.substringAfter("$bucketName/").substringBefore("?")
        val fullPath = "$bucketName/$path"
        
        val deleted = storedImages.remove(fullPath) != null
        
        if (deleted) {
            logger.info("Mock: Imagen eliminada de $fullPath")
        } else {
            logger.warn("Mock: Imagen no encontrada en $fullPath")
        }
        
        return deleted
    }
    
    /**
     * Verifica la conexión (siempre exitosa en mock)
     */
    fun testConnection(): String {
        return bucketName
    }
    
    /**
     * Obtiene el contenido de una imagen almacenada para verificación
     */
    fun getStoredImage(imageUrl: String): ByteArray? {
        val path = imageUrl.substringAfter("$bucketName/").substringBefore("?")
        val fullPath = "$bucketName/$path"
        return storedImages[fullPath]
    }
    
    /**
     * Obtiene el número de imágenes almacenadas
     */
    fun getStoredImageCount(): Int {
        return storedImages.size
    }
}