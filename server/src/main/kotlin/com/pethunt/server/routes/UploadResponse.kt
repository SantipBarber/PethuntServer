package com.pethunt.server.routes

import com.pethunt.server.services.StorageService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UploadResponse(
    val imageUrl: String,
    val success: Boolean
)

@Serializable
data class UrlUploadRequest(
    val imageUrl: String,
    val folder: String
)

fun Route.imageRoutes(storageService: StorageService) {
    authenticate("auth-jwt") {
        route("/images") {
            // Subir imagen desde archivo
            post("/upload") {
                try {
                    val multipart = call.receiveMultipart()
                    var folder = "general"
                    var imageData: ByteArray? = null
                    var contentType = ""
                    
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "folder") {
                                    folder = part.value
                                }
                            }
                            is PartData.FileItem -> {
                                contentType = part.contentType?.toString() ?: "image/jpeg"
                                imageData = part.streamProvider().readBytes()
                            }
                            else -> {}
                        }
                        part.dispose()
                    }
                    
                    if (imageData == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No image data provided"))
                        return@post
                    }
                    
                    val imageUrl = storageService.uploadImage(imageData!!, contentType, folder)
                    call.respond(UploadResponse(imageUrl, true))
                    
                } catch (e: Exception) {
                    call.application.log.error("Error uploading image", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error uploading image: ${e.message}")
                    )
                }
            }
            
            // Subir imagen desde URL
            post("/upload-url") {
                try {
                    val request = call.receive<UrlUploadRequest>()
                    val imageUrl = storageService.uploadImageFromUrl(request.imageUrl, request.folder)
                    
                    call.respond(UploadResponse(imageUrl, true))
                } catch (e: Exception) {
                    call.application.log.error("Error uploading image from URL", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error uploading image: ${e.message}")
                    )
                }
            }
            
            // Eliminar imagen
            delete("/{imageUrl}") {
                try {
                    val imageUrl = call.parameters["imageUrl"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Image URL required")
                    )
                    
                    val decoded = java.net.URLDecoder.decode(imageUrl, "UTF-8")
                    val deleted = storageService.deleteImage(decoded)
                    
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
                    }
                } catch (e: Exception) {
                    call.application.log.error("Error deleting image", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Error deleting image: ${e.message}")
                    )
                }
            }
        }
    }
}