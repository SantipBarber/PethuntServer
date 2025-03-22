package com.pethunt.server.services

import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class StorageServiceTest {
    private lateinit var storageService: StorageService
    
    @Before
    fun setup() {
        val config = MapApplicationConfig().apply {
            // Usa los mismos valores que en application.conf
            put("firebase.storageBucket", "pethuntserver.firebasestorage.app")
            put("firebase.credentials", "/Users/spbarber/Desarrollo/PetHuntProject/PethuntServer/server/src/main/resources/pethunt_server_firebase_config.json")
        }
        
        storageService = StorageService(config)
    }
    
    @Test
    fun `test connection to Firebase Storage`() {
        val bucketName = storageService.testConnection()
        assertEquals("pethuntserver.firebasestorage.app", bucketName)
    }
    
    @Test
    fun `test upload image`() {
        // Crear imagen de prueba
        val imageData = "Test image data".toByteArray()
        val contentType = "text/plain"
        val folder = "test"
        
        // Subir imagen
        val imageUrl = storageService.uploadImage(imageData, contentType, folder)
        
        // Verificar URL
        assertNotNull(imageUrl)
        assertTrue(imageUrl.contains("firebasestorage.googleapis.com"))
        
        println("Uploaded image URL: $imageUrl")
        
        // Limpiar - eliminar la imagen de prueba
        val deleted = storageService.deleteImage(imageUrl)
        assertTrue(deleted)
    }
    
    @Test
    fun `test upload image from URL`() {
        // URL de imagen pública
        val imageUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"
        val folder = "test"
        
        // Subir imagen desde URL
        val uploadedUrl = storageService.uploadImageFromUrl(imageUrl, folder)
        
        // Verificar URL
        assertNotNull(uploadedUrl)
        assertTrue(uploadedUrl.contains("firebasestorage.googleapis.com"))
        
        println("Uploaded image URL from external source: $uploadedUrl")
        
        // Limpiar - eliminar la imagen de prueba
        val deleted = storageService.deleteImage(uploadedUrl)
        assertTrue(deleted)
    }
    
    @After
    fun tearDown() {
        // Limpiar recursos si es necesario
    }
}