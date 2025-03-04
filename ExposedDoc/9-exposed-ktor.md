# Integración de Exposed con Ktor

Este documento explica cómo integrar Exposed con Ktor para desarrollar aplicaciones web con una capa de acceso a datos robusta. Ktor es un framework ligero para crear aplicaciones web asíncronas en Kotlin, y su combinación con Exposed permite desarrollar aplicaciones de principio a fin utilizando Kotlin.

## Configuración básica

### Dependencias necesarias

Para comenzar, necesitas añadir las dependencias adecuadas al archivo `build.gradle.kts`:

```kotlin
val exposedVersion = "0.46.0"
val ktorVersion = "2.3.7"
val hikariVersion = "5.0.1"

dependencies {
    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    
    // Connection pool
    implementation("com.zaxxer.hikari:HikariCP:$hikariVersion")
    
    // Database driver (ejemplo con PostgreSQL)
    implementation("org.postgresql:postgresql:42.7.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}
```

### Estructura de proyecto recomendada

Para mantener tu código organizado, considera una estructura similar a esta:

```
src/main/kotlin/
├── Application.kt             # Punto de entrada principal
├── config/
│   ├── DatabaseConfig.kt      # Configuración de Exposed
│   └── ApplicationConfig.kt   # Configuración de Ktor
├── models/                    # Definiciones de tablas y entidades
│   ├── Tables.kt
│   └── Entities.kt
├── repositories/              # Lógica de acceso a datos
│   ├── UserRepository.kt
│   └── PostRepository.kt
├── routes/                    # Rutas y endpoints de Ktor
│   ├── UserRoutes.kt
│   └── PostRoutes.kt
└── services/                  # Lógica de negocio
    ├── UserService.kt
    └── PostService.kt
```

## Configuración de Exposed en Ktor

### Configurar la conexión a la base de datos

El primer paso es configurar Exposed para conectarse a tu base de datos. Lo ideal es crear un módulo para encapsular esta configuración:

```kotlin
// DatabaseConfig.kt
package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val config = HikariConfig().apply {
        jdbcUrl = environment.config.property("database.url").getString()
        driverClassName = environment.config.property("database.driver").getString()
        username = environment.config.property("database.user").getString()
        password = environment.config.property("database.password").getString()
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    
    val dataSource = HikariDataSource(config)
    val database = Database.connect(dataSource)
    
    // Inicialización de la base de datos
    transaction {
        // Crear tablas, índices, etc.
        SchemaUtils.createMissingTablesAndColumns(Users, Posts, Comments)
    }
}
```

### Configuración de Ktor

Configura tu aplicación Ktor para usar la configuración de base de datos:

```kotlin
// Application.kt
package com.example

import com.example.config.configureDatabases
import com.example.routes.configureRouting
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureDatabases()
        install(ContentNegotiation) {
            jackson {}
        }
        configureRouting()
    }.start(wait = true)
}
```

### Configuración de Ktor con archivo de configuración

Es recomendable usar un archivo de configuración externo para los parámetros de la base de datos:

```hocon
# application.conf
ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
    application {
        modules = [ com.example.ApplicationKt.module ]
    }
}

database {
    url = "jdbc:postgresql://localhost:5432/mydb"
    url = ${?DATABASE_URL}
    driver = "org.postgresql.Driver"
    user = "postgres"
    user = ${?DATABASE_USER}
    password = "password"
    password = ${?DATABASE_PASSWORD}
}
```

Y luego en Application.kt:

```kotlin
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureDatabases()
    install(ContentNegotiation) {
        jackson {}
    }
    configureRouting()
}
```

## Transacciones con corrutinas en Ktor

### Transacciones suspendidas

Dado que Ktor es asíncrono y usa corrutinas, necesitas utilizar transacciones suspendidas de Exposed:

```kotlin
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun getUserById(id: Int): Map<String, Any?> = newSuspendedTransaction(Dispatchers.IO) {
    Users.select { Users.id eq id }
        .singleOrNull()
        ?.let {
            mapOf(
                "id" to it[Users.id],
                "name" to it[Users.name],
                "email" to it[Users.email]
            )
        }
}
```

### Implementando repositorios y servicios

Es recomendable organizar la lógica de acceso a datos en repositorios:

```kotlin
// UserRepository.kt
package com.example.repositories

import com.example.models.Users
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserRepository {
    suspend fun getAll(): List<Map<String, Any?>> = newSuspendedTransaction(Dispatchers.IO) {
        Users.selectAll()
            .map {
                mapOf(
                    "id" to it[Users.id],
                    "name" to it[Users.name],
                    "email" to it[Users.email]
                )
            }
    }
    
    suspend fun getById(id: Int): Map<String, Any?>? = newSuspendedTransaction(Dispatchers.IO) {
        Users.select { Users.id eq id }
            .singleOrNull()
            ?.let {
                mapOf(
                    "id" to it[Users.id],
                    "name" to it[Users.name],
                    "email" to it[Users.email]
                )
            }
    }
    
    suspend fun create(name: String, email: String): Map<String, Any?> = newSuspendedTransaction(Dispatchers.IO) {
        val id = Users.insert {
            it[Users.name] = name
            it[Users.email] = email
        } get Users.id
        
        mapOf(
            "id" to id,
            "name" to name,
            "email" to email
        )
    }
    
    suspend fun update(id: Int, name: String?, email: String?): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        Users.update({ Users.id eq id }) {
            name?.let { n -> it[Users.name] = n }
            email?.let { e -> it[Users.email] = e }
        } > 0
    }
    
    suspend fun delete(id: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        Users.deleteWhere { Users.id eq id } > 0
    }
}
```

### Servicios para la lógica de negocio

Los servicios encapsulan la lógica de negocio y utilizan los repositorios:

```kotlin
// UserService.kt
package com.example.services

import com.example.repositories.UserRepository

class UserService(private val repository: UserRepository) {
    suspend fun getAllUsers() = repository.getAll()
    
    suspend fun getUserById(id: Int) = repository.getById(id)
    
    suspend fun createUser(name: String, email: String): Map<String, Any?> {
        // Validación y lógica de negocio
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(email.matches(EMAIL_REGEX)) { "Invalid email format" }
        
        return repository.create(name, email)
    }
    
    suspend fun updateUser(id: Int, name: String?, email: String?): Boolean {
        // Validaciones
        email?.let { 
            require(it.matches(EMAIL_REGEX)) { "Invalid email format" }
        }
        
        return repository.update(id, name, email)
    }
    
    suspend fun deleteUser(id: Int) = repository.delete(id)
    
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@(.+)$")
    }
}
```

## Definiendo rutas en Ktor

### Rutas de usuario

```kotlin
// UserRoutes.kt
package com.example.routes

import com.example.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/users") {
        get {
            call.respond(userService.getAllUsers())
        }
        
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID format")
            
            val user = userService.getUserById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")
            
            call.respond(user)
        }
        
        post {
            try {
                val request = call.receive<CreateUserRequest>()
                val user = userService.createUser(request.name, request.email)
                call.respond(HttpStatusCode.Created, user)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An error occurred")
            }
        }
        
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID format")
            
            try {
                val request = call.receive<UpdateUserRequest>()
                
                if (userService.updateUser(id, request.name, request.email)) {
                    call.respond(HttpStatusCode.OK, "User updated successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An error occurred")
            }
        }
        
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID format")
            
            if (userService.deleteUser(id)) {
                call.respond(HttpStatusCode.OK, "User deleted successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }
    }
}

// DTOs
data class CreateUserRequest(val name: String, val email: String)
data class UpdateUserRequest(val name: String? = null, val email: String? = null)
```

### Configuración de rutas

```kotlin
// Routing.kt
package com.example.routes

import com.example.repositories.UserRepository
import com.example.services.UserService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // Crear instancias de repositorios
    val userRepository = UserRepository()
    
    // Crear instancias de servicios
    val userService = UserService(userRepository)
    
    routing {
        userRoutes(userService)
        // Otras rutas...
    }
}
```

## Uso de la API DAO con Ktor

Si prefieres usar la API DAO de Exposed, puedes integrarla con Ktor de manera similar:

### Definición de entidades

```kotlin
// Entities.kt
package com.example.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// Definición de tablas
object UsersTable : IntIdTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 100).uniqueIndex()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object PostsTable : IntIdTable("posts") {
    val title = varchar("title", 200)
    val content = text("content")
    val userId = reference("user_id", UsersTable)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

// Entidades
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    var createdAt by UsersTable.createdAt
    
    // Relación one-to-many
    val posts by Post referrersOn PostsTable.userId
}

class Post(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Post>(PostsTable)
    
    var title by PostsTable.title
    var content by PostsTable.content
    var user by User referencedOn PostsTable.userId
    var createdAt by PostsTable.createdAt
}
```

### Repositorio con DAO

```kotlin
// UserRepository.kt con API DAO
package com.example.repositories

import com.example.models.User
import com.example.models.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserRepository {
    suspend fun getAll(): List<UserDto> = newSuspendedTransaction(Dispatchers.IO) {
        User.all().map { it.toDto() }
    }
    
    suspend fun getById(id: Int): UserDto? = newSuspendedTransaction(Dispatchers.IO) {
        User.findById(id)?.toDto()
    }
    
    suspend fun create(name: String, email: String): UserDto = newSuspendedTransaction(Dispatchers.IO) {
        User.new {
            this.name = name
            this.email = email
        }.toDto()
    }
    
    suspend fun update(id: Int, name: String?, email: String?): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val user = User.findById(id) ?: return@newSuspendedTransaction false
        
        name?.let { user.name = it }
        email?.let { user.email = it }
        
        true
    }
    
    suspend fun delete(id: Int): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val user = User.findById(id) ?: return@newSuspendedTransaction false
        user.delete()
        true
    }
    
    // Extensión para convertir entidad a DTO
    private fun User.toDto() = UserDto(
        id = id.value,
        name = name,
        email = email,
        createdAt = createdAt
    )
}

// DTO para transferencia de datos
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime
)
```

## Manejo de concurrencia

Al trabajar con Ktor y Exposed en entornos concurrentes, es importante manejar adecuadamente las transacciones y los recursos:

### Pool de conexiones

El uso de un pool de conexiones como HikariCP es crucial:

```kotlin
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    driverClassName = "org.postgresql.Driver"
    username = "postgres"
    password = "password"
    maximumPoolSize = 20 // Ajusta según las necesidades y capacidad del servidor
    minimumIdle = 5
    idleTimeout = 30000
    connectionTimeout = 10000 // 10 segundos
    maxLifetime = 1800000 // 30 minutos
    isAutoCommit = false
}

val dataSource = HikariDataSource(config)
Database.connect(dataSource)
```

### Manejo de errores

Es importante manejar los errores correctamente, especialmente en operaciones asíncronas:

```kotlin
suspend fun getUsersSafely(): List<UserDto> {
    return try {
        newSuspendedTransaction(Dispatchers.IO) {
            User.all().map { it.toDto() }
        }
    } catch (e: Exception) {
        // Loguear el error
        logger.error("Error fetching users", e)
        
        // Devolver un valor por defecto o relanzar
        emptyList()
        // O: throw ServiceException("Failed to fetch users", e)
    }
}
```

## Testing

### Configuración para pruebas

Para pruebas, puedes usar una base de datos en memoria:

```kotlin
fun setupTestDatabase() = Database.connect(
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver"
)

class UserRepositoryTest {
    private lateinit var database: Database
    private lateinit var repository: UserRepository
    
    @BeforeEach
    fun setup() {
        database = setupTestDatabase()
        repository = UserRepository()
        
        transaction {
            SchemaUtils.create(UsersTable)
        }
    }
    
    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(UsersTable)
        }
    }
    
    @Test
    fun `test create user`() = runTest {
        // Given
        val name = "Test User"
        val email = "test@example.com"
        
        // When
        val result = repository.create(name, email)
        
        // Then
        assertEquals(name, result.name)
        assertEquals(email, result.email)
        
        // Verify in database
        val savedUser = transaction {
            User.find { UsersTable.email eq email }.singleOrNull()
        }
        assertNotNull(savedUser)
        assertEquals(name, savedUser?.name)
    }
}
```

### Pruebas de integración con Ktor y Exposed

```kotlin
class UserRoutesTest {
    private val userService = mockk<UserService>()
    private lateinit var testApp: TestApplication
    
    @BeforeEach
    fun setup() {
        testApp = TestApplication {
            application {
                install(ContentNegotiation) {
                    jackson {}
                }
                routing {
                    userRoutes(userService)
                }
            }
        }
        testApp.start()
    }
    
    @AfterEach
    fun tearDown() {
        testApp.stop()
    }
    
    @Test
    fun `test get all users`() = runTest {
        // Given
        val users = listOf(
            UserDto(1, "User 1", "user1@example.com", LocalDateTime.now()),
            UserDto(2, "User 2", "user2@example.com", LocalDateTime.now())
        )
        coEvery { userService.getAllUsers() } returns users
        
        // When
        val response = testApp.client.get("/users")
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(users, response.body<List<UserDto>>())
    }
}
```

## Patrones de diseño y mejores prácticas

### Inyección de dependencias

Aunque Ktor no tiene un sistema de inyección de dependencias integrado, puedes usar bibliotecas como Koin o implementar un patrón simple:

```kotlin
// ApplicationModules.kt
package com.example.config

import com.example.repositories.UserRepository
import com.example.services.UserService

class ApplicationModules {
    // Repositories
    val userRepository by lazy { UserRepository() }
    val postRepository by lazy { PostRepository() }
    
    // Services
    val userService by lazy { UserService(userRepository) }
    val postService by lazy { PostService(postRepository, userRepository) }
}

// Application.kt
fun Application.module() {
    // Crear e instanciar módulos
    val modules = ApplicationModules()
    
    configureDatabases()
    install(ContentNegotiation) {
        jackson {}
    }
    
    // Pasar servicios a las rutas
    routing {
        userRoutes(modules.userService)
        postRoutes(modules.postService)
    }
}
```

### Manejo de errores centralizado

```kotlin
// ExceptionHandling.kt
package com.example.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad Request")))
        }
        
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (cause.message ?: "Not Found")))
        }
        
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to (cause.message ?: "Forbidden")))
        }
        
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Internal Server Error")
            )
        }
    }
}

// Excepciones personalizadas
class NotFoundException(message: String) : RuntimeException(message)
class AuthorizationException(message: String) : RuntimeException(message)
```

## Ejemplos de aplicaciones completas

### API REST completa

```kotlin
// Application.kt
package com.example

import com.example.config.ApplicationModules
import com.example.config.configureDatabases
import com.example.config.configureExceptionHandling
import com.example.routes.configureRouting
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val modules = ApplicationModules()
    
    // Configuración de base de datos
    configureDatabases()
    
    // Manejo de errores
    configureExceptionHandling()
    
    // Plugins Ktor
    install(ContentNegotiation) {
        jackson {}
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
    
    install(Compression) {
        gzip()
    }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    
    // Configuración de rutas
    configureRouting(modules)
}
```

### Rendimiento y optimización

Para mejorar el rendimiento en aplicaciones Ktor con Exposed:

1. **Pool de conexiones adecuado**: Configura correctamente el tamaño del pool basado en la carga esperada.

2. **Transacciones limitadas**: Mantén las transacciones lo más cortas posible.

3. **Eager Loading**: Utiliza técnicas de eager loading para evitar el problema N+1.

```kotlin
suspend fun getPostsWithAuthors(): List<PostWithAuthorDto> = newSuspendedTransaction(Dispatchers.IO) {
    // Con DSL
    (Posts innerJoin Users)
        .select { Posts.userId eq Users.id }
        .map {
            PostWithAuthorDto(
                id = it[Posts.id],
                title = it[Posts.title],
                content = it[Posts.content],
                authorId = it[Users.id],
                authorName = it[Users.name]
            )
        }
    
    // Con DAO
    Post.all()
        .with(Post::user) // Eager loading
        .map {
            PostWithAuthorDto(
                id = it.id.value,
                title = it.title,
                content = it.content,
                authorId = it.user.id.value,
                authorName = it.user.name
            )
        }
}
```

4. **Caching**: Implementa caching cuando sea apropiado.

```kotlin
val cache = ConcurrentHashMap<Int, UserDto>()

suspend fun getUserByIdWithCache(id: Int): UserDto? {
    // Intentar obtener del cache
    cache[id]?.let { return it }
    
    // Si no está en cache, buscar en base de datos
    return newSuspendedTransaction(Dispatchers.IO) {
        User.findById(id)?.toDto()?.also {
            // Guardar en cache
            cache[id] = it
        }
    }
}
```

## Seguridad

### Autenticación y autorización

Implementación básica de JWT con Exposed:

```kotlin
// AuthRepository.kt
class AuthRepository {
    suspend fun getUserByCredentials(email: String, password: String): UserDto? = newSuspendedTransaction(Dispatchers.IO) {
        val user = User.find { UsersTable.email eq email }.singleOrNull() ?: return@newSuspendedTransaction null
        
        // Verificar contraseña (usando BCrypt)
        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return@newSuspendedTransaction null
        }
        
        user.toDto()
    }
}

// AuthService.kt
class AuthService(private val repository: AuthRepository) {
    private val issuer = "my-app"
    private val audience = "my-app-users"
    private val secret = "my-secret" // En producción, usar una clave segura
    private val validity = 3600L // 1 hora
    
    suspend fun generateToken(email: String, password: String): String? {
        val user = repository.getUserByCredentials(email, password) ?: return null
        
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", user.id)
            .withExpiresAt(Date(System.currentTimeMillis() + validity * 1000))
            .sign(Algorithm.HMAC256(secret))
    }
    
    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }
}

// AuthRoutes.kt
fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/login") {
            val credentials = call.receive<LoginRequest>()
            
            val token = authService.generateToken(credentials.email, credentials.password)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            
            call.respond(mapOf("token" to token))
        }
    }
}

// Proteger rutas
fun Route.securedRoutes(authService: AuthService, userService: UserService) {
    authenticate {
        route("/api") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()?.getClaim("userId", Int::class)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val user = userService.getUserById(userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")
                
                call.respond(user)
            }
        }
    }
}
```

## Resumen

La integración de Exposed con Ktor proporciona una base sólida para desarrollar aplicaciones web en Kotlin:

1. **Configuración**: Configura Exposed dentro de Ktor usando `configureDatabases()` y HikariCP para pooling de conexiones.

2. **Transacciones asíncronas**: Usa `newSuspendedTransaction` para operaciones de base