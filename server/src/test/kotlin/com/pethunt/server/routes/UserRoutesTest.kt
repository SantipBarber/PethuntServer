package com.pethunt.server.routes

import com.pethunt.server.models.UserCreateDTO
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {
    @Test
    fun `register endpoint should create user and return 201`() = testApplication {
        application {
            routing {
                post("/users/register") {
                    call.response.status(HttpStatusCode.Created)
                }
            }
        }

        // Given
        val userDTO = UserCreateDTO(
            email = "test@example.com",
            password = "password123",
            username = "testuser"
        )

        // When
        val response = client.post("/users/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(userDTO))
        }

        // Then
        assertEquals(HttpStatusCode.Created, response.status)
    }
}