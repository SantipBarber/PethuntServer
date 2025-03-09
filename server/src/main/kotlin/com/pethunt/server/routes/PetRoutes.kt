package com.pethunt.server.routes

import com.pethunt.server.models.PetCreateDTO
import com.pethunt.server.services.PetService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

fun Route.petRoutes(petService: PetService) {
    // Rutas protegidas con autenticación JWT
    authenticate("auth-jwt") {
        route("/pets") {
            // Crear una nueva mascota
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    
                    val petDTO = call.receive<PetCreateDTO>()
                    val pet = petService.createPet(UUID.fromString(userId), petDTO)
                    
                    call.respond(HttpStatusCode.Created, pet)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error creating pet", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
            
            // Obtener todas las mascotas del usuario autenticado
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                    
                    val pets = petService.getPetsByUserId(UUID.fromString(userId), limit, offset)
                    
                    call.respond(pets)
                } catch (e: Exception) {
                    call.application.log.error("Error getting user pets", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
            
            // Obtener una mascota específica
            get("/{id}") {
                try {
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))
                    
                    val pet = petService.getPetById(UUID.fromString(id))
                    
                    if (pet != null) {
                        call.respond(pet)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Mascota no encontrada"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de mascota inválido"))
                } catch (e: Exception) {
                    call.application.log.error("Error getting pet", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
            
            // Actualizar una mascota
            put("/{id}") {
                try {
                    val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                        ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    
                    val petId = UUID.fromString(id)
                    
                    // Verificar que el usuario es el propietario de la mascota
                    if (!petService.isOwner(petId, UUID.fromString(userId))) {
                        return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
                    }
                    
                    val petDTO = call.receive<PetCreateDTO>()
                    val updatedPet = petService.updatePet(petId, petDTO)
                    
                    if (updatedPet != null) {
                        call.respond(updatedPet)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Mascota no encontrada"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error updating pet", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
            
            // Eliminar una mascota
            delete("/{id}") {
                try {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    
                    val petId = UUID.fromString(id)
                    
                    // Verificar que el usuario es el propietario de la mascota
                    if (!petService.isOwner(petId, UUID.fromString(userId))) {
                        return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
                    }
                    
                    val deleted = petService.deletePet(petId)
                    
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Mascota no encontrada"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de mascota inválido"))
                } catch (e: Exception) {
                    call.application.log.error("Error deleting pet", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
            
            // Actualizar imagen de perfil de una mascota
            post("/{id}/profile-image") {
                try {
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID requerido"))
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    
                    val petId = UUID.fromString(id)
                    
                    // Verificar que el usuario es el propietario de la mascota
                    if (!petService.isOwner(petId, UUID.fromString(userId))) {
                        return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
                    }
                    
                    val request = call.receive<ProfileImageRequest>()
                    val updated = petService.updatePetProfileImage(petId, request.imageUrl)
                    
                    if (updated) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Imagen de perfil actualizada"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Mascota no encontrada"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.application.log.error("Error updating pet profile image", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
            
            // Obtener el número de mascotas de un usuario
            get("/count") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    
                    val count = petService.countPetsByUserId(UUID.fromString(userId))
                    
                    call.respond(mapOf("count" to count))
                } catch (e: Exception) {
                    call.application.log.error("Error counting pets", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
                }
            }
        }
    }
}

@Serializable
data class ProfileImageRequest(val imageUrl: String)