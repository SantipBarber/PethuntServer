# Transacciones y concurrencia en Exposed

Las transacciones son una parte fundamental del trabajo con bases de datos, ya que garantizan la atomicidad, consistencia, aislamiento y durabilidad (ACID) de las operaciones. Este documento explica cómo trabajar con transacciones en Exposed, tanto en aplicaciones síncronas como asíncronas con corrutinas.

## Transacciones básicas

En Exposed, todas las operaciones de base de datos deben ejecutarse dentro de un bloque `transaction`:

```kotlin
transaction {
    // Operaciones de base de datos aquí
    val user = Users.insert {
        it[name] = "Juan"
        it[email] = "juan@example.com"
    }
    
    Posts.insert {
        it[title] = "Mi post"
        it[content] = "Contenido"
        it[userId] = user[Users.id]
    }
    
    // Si todo va bien, se hace commit automáticamente al final
    // Si hay una excepción, se hace rollback automáticamente
}
```

Los bloques `transaction` aseguran que:
- Si todas las operaciones tienen éxito, los cambios se guardan (commit)
- Si ocurre algún error, todos los cambios se revierten (rollback)
- Las operaciones dentro del bloque se ejecutan como una unidad atómica

## Configuración de transacciones

Puedes personalizar el comportamiento de una transacción mediante parámetros:

```kotlin
transaction(
    transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
    readOnly = true,
    db = myDatabase
) {
    // Operaciones aquí, en modo solo lectura con nivel de aislamiento SERIALIZABLE
    val users = Users.selectAll().toList()
}
```

### Niveles de aislamiento

El parámetro `transactionIsolation` define cómo se comportan las transacciones concurrentes:

- `TRANSACTION_NONE`: No hay soporte para transacciones.
- `TRANSACTION_READ_UNCOMMITTED`: Permite leer cambios no confirmados (dirty reads).
- `TRANSACTION_READ_COMMITTED`: Evita lecturas sucias, pero permite lecturas no repetibles.
- `TRANSACTION_REPEATABLE_READ`: Evita lecturas sucias y no repetibles, pero permite lecturas fantasma.
- `TRANSACTION_SERIALIZABLE`: El nivel más estricto. Evita todos los problemas de concurrencia.

El nivel predeterminado es `TRANSACTION_REPEATABLE_READ`.

### Modo solo lectura

El parámetro `readOnly = true` indica que la transacción no modificará datos:

```kotlin
transaction(readOnly = true) {
    // Solo operaciones de lectura
    val users = Users.selectAll().toList()
}
```

Este modo puede mejorar el rendimiento en algunas bases de datos y sirve como protección para evitar operaciones de escritura accidentales.

### Selección de base de datos

El parámetro `db` permite especificar qué conexión de base de datos usar:

```kotlin
// Configuración
val db1 = Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
val db2 = Database.connect("jdbc:mysql://localhost:3306/db2", "com.mysql.jdbc.Driver", "root", "password")

// Uso
transaction(db = db1) {
    // Operaciones en db1
}

transaction(db = db2) {
    // Operaciones en db2
}
```

## Retorno de valores desde transacciones

Las transacciones pueden devolver valores, lo que facilita el trabajo con los resultados:

```kotlin
val users = transaction {
    Users.selectAll().map { 
        mapOf(
            "id" to it[Users.id],
            "name" to it[Users.name],
            "email" to it[Users.email]
        )
    }
}

// Usar los resultados fuera de la transacción
println("Encontrados ${users.size} usuarios")
```

## Manejo de excepciones

Puedes manejar excepciones dentro de una transacción, lo que te da control sobre el proceso de rollback:

```kotlin
try {
    transaction {
        val user = Users.insert {
            it[name] = "Juan"
            it[email] = "juan@example.com"
        }
        
        if (someCondition) {
            // Provocar rollback explícito
            throw Exception("Error controlado")
        }
        
        Posts.insert {
            it[title] = "Mi post"
            it[content] = "Contenido"
            it[userId] = user[Users.id]
        }
    }
} catch (e: Exception) {
    println("La transacción falló: ${e.message}")
}
```

## Rollback manual

Puedes forzar un rollback manualmente con la función `rollback()`:

```kotlin
transaction {
    val user = Users.insert {
        it[name] = "Juan"
        it[email] = "juan@example.com"
    }
    
    if (someCondition) {
        rollback() // Deshacer todos los cambios
        return@transaction null // Opcional: devolver un valor
    }
    
    // Esta parte no se ejecutará si se llamó a rollback()
    Posts.insert {
        it[title] = "Mi post"
        it[content] = "Contenido"
        it[userId] = user[Users.id]
    }
}
```

La función `rollback()` es útil cuando quieres deshacer cambios basados en una condición lógica, no necesariamente por un error.

## Transacciones anidadas

Exposed permite anidar transacciones:

```kotlin
transaction {
    println("Transacción exterior iniciada")
    
    val user = Users.insert {
        it[name] = "Usuario Exterior"
        it[email] = "exterior@example.com"
    }
    
    transaction {
        println("Transacción anidada iniciada")
        
        Posts.insert {
            it[title] = "Post desde transacción anidada"
            it[content] = "Contenido"
            it[userId] = user[Users.id]
        }
        
        // Si hay rollback aquí, también afecta a la transacción exterior por defecto
    }
    
    println("Transacción exterior continuando")
}
```

### Comportamiento predeterminado

Por defecto, las transacciones anidadas comparten recursos con su transacción padre. Esto significa:

1. Un rollback en la transacción anidada también provoca rollback en la transacción padre
2. Las transacciones anidadas no son independientes

### Transacciones anidadas independientes

Si deseas que las transacciones anidadas sean independientes (usando SAVEPOINT), configura tu base de datos:

```kotlin
val db = Database.connect(/* configuración */)
db.useNestedTransactions = true

// Ahora las transacciones anidadas pueden hacer rollback independientemente
transaction {
    val user = Users.insert { /* ... */ }
    
    try {
        transaction {
            // Si esto falla, solo afecta a esta transacción anidada
            Posts.insert { /* ... */ }
            
            if (problemDetected) {
                rollback()
            }
        }
    } catch (e: Exception) {
        // La inserción del usuario aún se mantiene
        println("Error en transacción anidada: ${e.message}")
    }
    
    // Esta parte se ejecuta incluso si la transacción anidada falló
    println("Usuario creado: ${user[Users.id]}")
}
```

Con `useNestedTransactions = true`, Exposed utiliza la funcionalidad SAVEPOINT de SQL para marcar la transacción actual al comienzo de un bloque de transacción anidada, permitiendo hacer rollback solo hasta ese punto.

## Retry automático de transacciones

Exposed permite reintentar transacciones automáticamente cuando ocurren excepciones:

```kotlin
transaction {
    // Configura el número máximo de intentos
    maxAttempts = 3
    
    // Configura los delays entre intentos (en milisegundos)
    minRetryDelay = 100
    maxRetryDelay = 500
    
    // Operaciones que podrían fallar por problemas temporales
    Users.insert { /* ... */ }
}
```

Esto es útil para manejar errores transitorios como deadlocks o problemas de conexión.

### Configuración global

También puedes configurar estos valores a nivel global:

```kotlin
val db = Database.connect(
    url = "jdbc:mysql://localhost:3306/mydb",
    driver = "com.mysql.jdbc.Driver",
    user = "root",
    password = "password",
    databaseConfig = DatabaseConfig {
        defaultMaxAttempts = 3
        defaultMinRetryDelay = 100
        defaultMaxRetryDelay = 500
    }
)
```

## Transacciones con corrutinas

Exposed soporta transacciones en aplicaciones asíncronas mediante corrutinas de Kotlin.

### newSuspendedTransaction

La función `newSuspendedTransaction` permite ejecutar transacciones en un contexto de corrutina:

```kotlin
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun getUsers(): List<Map<String, Any>> {
    return newSuspendedTransaction(Dispatchers.IO) {
        Users.selectAll().map { 
            mapOf(
                "id" to it[Users.id],
                "name" to it[Users.name],
                "email" to it[Users.email]
            ) 
        }
    }
}

// Uso
suspend fun main() {
    val users = getUsers()
    println("Usuarios: $users")
}
```

La función `newSuspendedTransaction` siempre ejecuta una nueva transacción, independientemente del contexto de transacción actual. Esto evita problemas de concurrencia cuando el orden de ejecución podría cambiar debido al `CoroutineDispatcher`.

### suspendedTransactionAsync

Para ejecutar una transacción de forma asíncrona y obtener un resultado futuro:

```kotlin
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

suspend fun calculateAnalytics(): Map<String, Int> {
    val postsCountDeferred = suspendedTransactionAsync(Dispatchers.IO) {
        Posts.selectAll().count()
    }
    
    val usersCountDeferred = suspendedTransactionAsync(Dispatchers.IO) {
        Users.selectAll().count()
    }
    
    // Esperar ambos resultados
    val postsCount = postsCountDeferred.await()
    val usersCount = usersCountDeferred.await()
    
    return mapOf(
        "posts" to postsCount,
        "users" to usersCount
    )
}
```

### Transacciones anidadas con corrutinas

Para transacciones anidadas dentro de corrutinas, usa `withSuspendTransaction`:

```kotlin
suspend fun complexOperation() {
    newSuspendedTransaction {
        val user = Users.insert { /* ... */ }
        
        // Transacción anidada
        withSuspendTransaction {
            Posts.insert {
                it[userId] = user[Users.id]
                /* ... */
            }
        }
    }
}
```

### Mejores prácticas con corrutinas

1. **Usar el dispatcher adecuado**: Normalmente `Dispatchers.IO` para operaciones de base de datos
2. **Manejar excepciones**: Las transacciones suspendidas pueden lanzar excepciones
3. **Evitar bloqueos**: No mezclar código bloqueante con corrutinas

```kotlin
launch {
    try {
        val result = newSuspendedTransaction(Dispatchers.IO) {
            // Operaciones de base de datos
            Users.select { Users.id eq userId }.singleOrNull()
        }
        
        if (result == null) {
            throw NoSuchElementException("Usuario no encontrado")
        }
        
        // Procesar resultado
    } catch (e: Exception) {
        // Manejar error
    }
}
```

## Timeouts de consultas

Puedes establecer un timeout para las operaciones dentro de una transacción:

```kotlin
transaction {
    // Establece timeout de 3 segundos
    queryTimeout = 3
    
    try {
        // Consulta potencialmente lenta
        val result = exec("SELECT * FROM large_table WHERE complex_condition")
    } catch (e: SQLException) {
        // Manejar timeout u otros errores SQL
    }
}
```

## Gestión de conexiones

### Conexiones explícitas

Si necesitas controlar manualmente las conexiones JDBC:

```kotlin
transaction {
    // Obtener la conexión JDBC subyacente
    val connection = connection
    
    // Usar la conexión directamente si es necesario
    connection.autoCommit = false
    
    // Crear un statement JDBC directamente
    val statement = connection.createStatement()
    // ...
}
```

### Pool de conexiones con HikariCP

Para aplicaciones en producción, es recomendable usar un pool de conexiones:

```kotlin
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun createHikariDataSource(): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://localhost:3306/mydb"
        driverClassName = "com.mysql.jdbc.Driver"
        username = "root"
        password = "password"
        maximumPoolSize = 10
        connectionTimeout = 30000
        idleTimeout = 600000
        maxLifetime = 1800000
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }
    
    return HikariDataSource(config)
}

// Configurar Exposed con el pool de conexiones
val dataSource = createHikariDataSource()
val db = Database.connect(dataSource)
```

## Integración con Ktor

Para integrar Exposed con Ktor de manera eficiente:

```kotlin
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun main() {
    // Configurar pool de conexiones
    val dataSource = createHikariDataSource()
    val database = Database.connect(dataSource)
    
    // Configurar y arrancar el servidor Ktor
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/users") {
                val users = newSuspendedTransaction(Dispatchers.IO, database) {
                    Users.selectAll().map { 
                        mapOf(
                            "id" to it[Users.id],
                            "name" to it[Users.name],
                            "email" to it[Users.email]
                        ) 
                    }
                }
                
                call.respond(users)
            }
            
            get("/user/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID inválido")
                    return@get
                }
                
                val user = newSuspendedTransaction(Dispatchers.IO, database) {
                    Users.select { Users.id eq id }.singleOrNull()?.let {
                        mapOf(
                            "id" to it[Users.id],
                            "name" to it[Users.name],
                            "email" to it[Users.email]
                        )
                    }
                }
                
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                }
            }
        }
    }.start(wait = true)
}
```

### Módulo de extensión para Ktor

Puedes crear un módulo para encapsular la lógica de la base de datos:

```kotlin
fun Application.configureDatabases() {
    // Configurar base de datos
    val dataSource = createHikariDataSource()
    val database = Database.connect(dataSource)
    
    // Crear tablas si es necesario
    transaction(database) {
        SchemaUtils.create(Users, Posts)
    }
}

fun Application.configureRouting() {
    routing {
        userRoutes()
        postRoutes()
    }
}

fun Route.userRoutes() {
    route("/users") {
        // Definir rutas para usuarios
    }
}
```

## Concurrencia y bloqueo optimista

Exposed no proporciona directamente mecanismos de bloqueo optimista, pero puedes implementarlos:

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    val version = integer("version").default(1) // Columna de versión
    
    override val primaryKey = PrimaryKey(id)
}

fun updateUserOptimistic(userId: Int, newName: String): Boolean {
    return transaction {
        // Leer versión actual
        val user = Users.select { Users.id eq userId }.singleOrNull()
            ?: return@transaction false
        
        val currentVersion = user[Users.version]
        
        // Intentar actualizar con control de versión
        val updatedRows = Users.update({ 
            (Users.id eq userId) and (Users.version eq currentVersion) 
        }) {
            it[name] = newName
            it[version] = currentVersion + 1
        }
        
        // Si updatedRows es 0, significa que alguien más modificó el registro
        return@transaction updatedRows > 0
    }
}
```

Este enfoque evita conflictos de actualización concurrente mediante una columna de versión.

## Concurrencia y bloqueo pesimista

Algunas bases de datos soportan bloqueo pesimista con `SELECT FOR UPDATE`:

```kotlin
fun updateUserPessimistic(userId: Int, newName: String): Boolean {
    return transaction {
        // Bloquear la fila para otros lectores
        val user = exec(
            "SELECT * FROM users WHERE id = ? FOR UPDATE", 
            listOf(IntegerColumnType() to userId)
        ) { rs ->
            if (rs.next()) {
                mapOf(
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("name"),
                    "version" to rs.getInt("version")
                )
            } else null
        } ?: return@transaction false
        
        // Ahora podemos actualizar con seguridad porque tenemos un bloqueo exclusivo
        Users.update({ Users.id eq userId }) {
            it[name] = newName
            it[version] = user["version"] as Int + 1
        }
        
        return@transaction true
    }
}
```

## Resumen

Las transacciones en Exposed proporcionan:

1. **Atomicidad**: Las operaciones se ejecutan como una unidad atómica.
2. **Configuración flexible**: Puedes personalizar el nivel de aislamiento y más.
3. **Control de errores**: Manejo de excepciones y reintentos automáticos.
4. **Soporte para corrutinas**: Integración con el modelo asíncrono de Kotlin.
5. **Transacciones anidadas**: Posibilidad de controlar el alcance de los rollbacks.

Puntos clave:
- Usa `transaction` para operaciones síncronas.
- Usa `newSuspendedTransaction` para contextos de corrutinas.
- Configura un pool de conexiones para aplicaciones en producción.
- Implementa estrategias de concurrencia según tus necesidades.
- Para aplicaciones Ktor, organiza tu código en módulos y utiliza corrutinas para operaciones de base de datos.