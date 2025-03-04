# Mejores prácticas y patrones con Exposed

Este documento resume las mejores prácticas y patrones recomendados para trabajar con Exposed en aplicaciones Kotlin.

## Estructura del proyecto

### Organización recomendada

```
src/main/kotlin/
├── config/              # Configuración de base de datos
├── models/              # Definiciones de tablas y entidades
├── repositories/        # Lógica de acceso a datos
├── services/            # Lógica de negocio
└── Application.kt       # Punto de entrada
```

### Separación de responsabilidades

Sigue el patrón de repositorio para organizar el código:

```kotlin
// Tabla y entidad
object UsersTable : IntIdTable() {
    val name = varchar("name", 50)
    val email = varchar("email", 100)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    var name by UsersTable.name
    var email by UsersTable.email
}

// Repositorio
class UserRepository {
    fun getById(id: Int): User? = transaction {
        User.findById(id)
    }
    
    fun create(name: String, email: String): User = transaction {
        User.new {
            this.name = name
            this.email = email
        }
    }
}

// Servicio
class UserService(private val repository: UserRepository) {
    fun createUser(name: String, email: String): User {
        require(name.isNotBlank()) { "Name cannot be blank" }
        return repository.create(name, email)
    }
}
```

## Manejo de conexiones

### Configuración con pool de conexiones

Usa siempre un pool de conexiones como HikariCP para aplicaciones en producción:

```kotlin
fun setupDatabase() {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "password"
        maximumPoolSize = 10
        minimumIdle = 5
        idleTimeout = 30000 // 30 segundos
        connectionTimeout = 10000 // 10 segundos
        maxLifetime = 1800000 // 30 minutos
    }
    
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
}
```

### Evitar múltiples conexiones

Evita llamar a `Database.connect()` múltiples veces. En su lugar, guarda una referencia a la base de datos:

```kotlin
object DatabaseFactory {
    private var initialized = false
    
    fun init() {
        if (!initialized) {
            val dataSource = createHikariDataSource()
            Database.connect(dataSource)
            initialized = true
        }
    }
}
```

## Transacciones

### Mantener transacciones cortas

Minimiza el trabajo realizado dentro de las transacciones:

```kotlin
// INCORRECTO
fun processLargeDataset(data: List<UserData>) = transaction {
    data.forEach { processItem(it) } // Esto mantiene la transacción abierta por mucho tiempo
}

// CORRECTO
fun processLargeDataset(data: List<UserData>) {
    data.forEach { 
        transaction {
            processItem(it) // Transacción corta por cada elemento
        }
    }
}
```

### Manejo explícito de excepciones

```kotlin
fun tryOperation(): Result<User> {
    return try {
        val user = transaction {
            User.new { name = "Test" }
        }
        Result.success(user)
    } catch (e: Exception) {
        log.error("Error creating user", e)
        Result.failure(e)
    }
}
```

## Consultas eficientes

### Seleccionar solo las columnas necesarias

```kotlin
val userNames = transaction {
    // Solo selecciona la columna 'name'
    Users.slice(Users.name).selectAll().map { it[Users.name] }
}
```

### Eager loading para evitar N+1

```kotlin
// Problema N+1
val posts = transaction {
    User.all().flatMap { user ->
        user.posts.toList() // Una consulta adicional por usuario
    }
}

// Solución con eager loading
val posts = transaction {
    User.all().with(User::posts).flatMap { user -> 
        user.posts.toList() // No genera consultas adicionales
    }
}
```

### Optimización de consultas de conteo

```kotlin
// Menos eficiente
val count = transaction {
    Users.selectAll().count()
}

// Más eficiente
val count = transaction {
    Users.slice(Users.id.count()).selectAll().single()[Users.id.count()]
}
```

## Seguridad

### Parámetros seguros

No concatenes valores directamente en consultas SQL:

```kotlin
// INCORRECTO - Vulnerable a inyección SQL
val userName = "O'Connor"
exec("SELECT * FROM users WHERE name = '$userName'")

// CORRECTO - Parámetros protegidos
Users.select { Users.name eq userName }

// Si necesitas SQL crudo, usa parámetros preparados
exec("SELECT * FROM users WHERE name = ?", listOf(
    StringColumnType() to userName
))
```

### Datos sensibles

Nunca almacenes contraseñas en texto plano:

```kotlin
fun createUser(email: String, password: String) = transaction {
    User.new {
        this.email = email
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
```

## Testing

### Base de datos en memoria para pruebas

```kotlin
class UserRepositoryTest {
    private lateinit var database: Database
    
    @BeforeEach
    fun setup() {
        // H2 en memoria para pruebas
        database = Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        
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
    fun testCreateUser() {
        // Test code
    }
}
```

### Datos de prueba

```kotlin
fun insertTestData() = transaction {
    // Limpiar datos existentes
    SchemaUtils.drop(Users, Posts)
    SchemaUtils.create(Users, Posts)
    
    // Insertar datos de prueba
    val user1 = User.new {
        name = "Test User 1"
        email = "test1@example.com"
    }
    
    val user2 = User.new {
        name = "Test User 2"
        email = "test2@example.com"
    }
    
    Post.new {
        title = "Test Post 1"
        content = "Test content"
        user = user1
    }
}
```

## Rendimiento

### Operaciones por lotes

Para inserciones/actualizaciones masivas, usa operaciones por lotes:

```kotlin
transaction {
    val users = (1..1000).map { i ->
        Triple("User $i", "user$i@example.com", i % 100)
    }
    
    Users.batchInsert(users) { (name, email, age) ->
        this[Users.name] = name
        this[Users.email] = email
        this[Users.age] = age
    }
}
```

### Índices adecuados

Crea índices para las columnas usadas en consultas frecuentes:

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 100)
    val name = varchar("name", 50)
    
    init {
        uniqueIndex(email) // Para búsquedas por email
        index(name) // Para búsquedas por nombre
    }
    
    override val primaryKey = PrimaryKey(id)
}
```

### Paginación

Implementa paginación para conjuntos de datos grandes:

```kotlin
fun getPagedUsers(page: Int, pageSize: Int): List<User> = transaction {
    User.all()
        .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
        .toList()
}
```

## Organización del código

### Extensiones útiles

Crea extensiones para simplificar tareas comunes:

```kotlin
// Extensión para convertir ResultRow a DTO
fun ResultRow.toUserDto() = UserDto(
    id = this[Users.id],
    name = this[Users.name],
    email = this[Users.email]
)

// Uso
val users = transaction {
    Users.selectAll().map { it.toUserDto() }
}
```

### Constantes para tamaños de columna

Define constantes para los tamaños de columnas:

```kotlin
object DatabaseConstants {
    const val SHORT_TEXT_LENGTH = 50
    const val MEDIUM_TEXT_LENGTH = 100
    const val LONG_TEXT_LENGTH = 500
}

object Users : Table() {
    val name = varchar("name", DatabaseConstants.SHORT_TEXT_LENGTH)
    val email = varchar("email", DatabaseConstants.MEDIUM_TEXT_LENGTH)
}
```

## Integración con frameworks

### Spring Boot

Usa el starter de Exposed para Spring Boot:

```kotlin
dependencies {
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
}
```

Esto configura automáticamente Exposed con el DataSource de Spring.

### Ktor

Para Ktor, encapsula la configuración de Exposed:

```kotlin
fun Application.configureDatabases() {
    val dataSource = createHikariDataSource(environment.config)
    Database.connect(dataSource)
    
    transaction {
        SchemaUtils.createMissingTablesAndColumns(Users, Posts)
    }
}
```

## Errores comunes a evitar

1. **Mantener transacciones abiertas demasiado tiempo**: Las transacciones deben ser cortas y específicas.

2. **No configurar un pool de conexiones**: Siempre usa un pool de conexiones para aplicaciones reales.

3. **Problema N+1**: Utiliza eager loading (`with()`) para evitar múltiples consultas.

4. **No manejar excepciones**: Las operaciones de base de datos pueden fallar, maneja las excepciones adecuadamente.

5. **No cerrar recursos**: Asegúrate de cerrar recursos como Statement y ResultSet cuando uses JDBC directamente.

6. **Consultas ineficientes**: Selecciona solo las columnas necesarias y usa los índices adecuados.

7. **Operaciones bloqueantes en contextos no bloqueantes**: Usa `newSuspendedTransaction` con corrutinas.