package com.pethunt.server.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.ktor.server.config.*
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

class StorageService(config: ApplicationConfig) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val storage: Storage
    private val bucketName: String
    
    init {
        val credentialsPath = config.propertyOrNull("firebase.credentials")?.getString()
        val credentials = if (credentialsPath != null) {
            GoogleCredentials.fromStream(FileInputStream(credentialsPath))
        } else {
            // Usar credenciales por defecto (entorno o cuenta de servicio)
            GoogleCredentials.getApplicationDefault()
        }
        
        storage = StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service
            
        bucketName = config.property("firebase.storageBucket").getString()
        logger.info("Initialized Firebase Storage with bucket: $bucketName")
    }
    
    /**
     * Sube una imagen al almacenamiento y devuelve su URL
     */
    fun uploadImage(data: ByteArray, contentType: String, folder: String): String {
        val fileName = "${folder}/${UUID.randomUUID()}"
        
        return try {
            val blobId = BlobId.of(bucketName, fileName)
            val blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build()
                
            val blob = storage.create(blobInfo, data)
            
            // Generar URL firmada para acceso temporal (1 semana)
            blob.signUrl(7, TimeUnit.DAYS).toString()
        } catch (e: Exception) {
            logger.error("Error uploading image to Firebase Storage: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Sube una imagen al almacenamiento a partir de una URL
     */
    fun uploadImageFromUrl(imageUrl: String, folder: String): String {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            val contentType = connection.contentType
            
            val imageData = url.openStream().use { it.readBytes() }
            
            return uploadImage(imageData, contentType, folder)
        } catch (e: Exception) {
            logger.error("Error downloading image from URL: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Elimina una imagen del almacenamiento
     */
    fun deleteImage(imageUrl: String): Boolean {
        try {
            // Extraer el nombre del archivo de la URL
            val path = imageUrl.substringAfter("$bucketName/").substringBefore("?")
            val blobId = BlobId.of(bucketName, path)
            
            return storage.delete(blobId)
        } catch (e: Exception) {
            logger.error("Error deleting image: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Genera URLs firmadas para imágenes existentes
     */
    fun generateSignedUrl(path: String, expirationDays: Long = 7): String {
        try {
            val blobId = BlobId.of(bucketName, path)
            val blob = storage.get(blobId)
            
            return blob?.signUrl(expirationDays, TimeUnit.DAYS)?.toString()
                ?: throw IllegalArgumentException("Image not found: $path")
        } catch (e: Exception) {
            logger.error("Error generating signed URL: ${e.message}", e)
            throw e
        }
    }

    fun testConnection(): String {
        try {
            // Intenta acceder al bucket para verificar la conexión
            val bucket = storage.get(bucketName)
            return if (bucket != null) {
                bucketName
            } else {
                throw IllegalStateException("Bucket not found: $bucketName")
            }
        } catch (e: Exception) {
            logger.error("Firebase Storage connection test failed", e)
            throw e
        }
    }
}