package com.pethunt.server.services

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StorageServiceTest {
    private lateinit var mockStorageService: MockStorageService

    @Before
    fun setup() {
        // Inicializar el mock de StorageService directamente
        mockStorageService = MockStorageService()
    }

    @Test
    fun `test connection to storage`() {
        val bucketName = mockStorageService.testConnection()
        assertEquals("mock-bucket", bucketName)
    }

    @Test
    fun `test upload image`() {
        // Crear datos de prueba
        val imageData = "Test image data".toByteArray()
        val contentType = "text/plain"
        val folder = "test"

        // Subir imagen usando el mock
        val imageUrl = mockStorageService.uploadImage(imageData, contentType, folder)

        // Verificar URL
        assertNotNull(imageUrl)
        assertTrue(imageUrl.contains("firebasestorage.example.com"))
        assertTrue(imageUrl.contains("mock-bucket"))
        assertTrue(imageUrl.contains(folder))

        // Verificar que los datos se almacenaron
        val storedData = mockStorageService.getStoredImage(imageUrl)
        assertNotNull(storedData)
        assertEquals(String(imageData), String(storedData))
    }

    @Test
    fun `test upload image from URL`() {
        // URL de imagen de prueba
        val imageUrl = "https://example.com/test.jpg"
        val folder = "test"

        // Subir imagen desde URL usando el mock
        val uploadedUrl = mockStorageService.uploadImageFromUrl(imageUrl, folder)

        // Verificar URL
        assertNotNull(uploadedUrl)
        assertTrue(uploadedUrl.contains("firebasestorage.example.com"))
        assertTrue(uploadedUrl.contains("mock-bucket"))
        assertTrue(uploadedUrl.contains(folder))

        // Verificar que se almacenaron datos
        val storedData = mockStorageService.getStoredImage(uploadedUrl)
        assertNotNull(storedData)
    }

    @Test
    fun `test delete image`() {
        // Primero subir una imagen
        val imageData = "Image to delete".toByteArray()
        val uploadedUrl = mockStorageService.uploadImage(imageData, "text/plain", "test")

        // Verificar que se almacenó
        assertEquals(1, mockStorageService.getStoredImageCount())

        // Eliminar la imagen
        val deleted = mockStorageService.deleteImage(uploadedUrl)
        assertTrue(deleted)

        // Verificar que se eliminó
        assertEquals(0, mockStorageService.getStoredImageCount())
    }
}