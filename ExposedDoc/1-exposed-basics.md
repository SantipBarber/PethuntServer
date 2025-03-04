# Fundamentos de Exposed

## Introducción a Exposed

Exposed es una biblioteca ORM (Object-Relational Mapping) ligera para Kotlin que proporciona una capa de abstracción sobre drivers JDBC. Su diseño aprovecha las características del lenguaje Kotlin para ofrecer una API type-safe y expresiva para trabajar con bases de datos relacionales.

La mascota oficial de Exposed es la sepia (cuttlefish), conocida por su extraordinaria capacidad de mimetismo que le permite adaptarse a cualquier entorno. De manera similar, Exposed puede utilizarse para trabajar con diversos motores de bases de datos, ayudándote a construir aplicaciones sin dependencias específicas de un motor en particular.

## Dos APIs distintas

Exposed ofrece dos enfoques para el acceso a bases de datos:

### API DSL (Domain-Specific Language)

La API DSL proporciona una abstracción basada en Kotlin para interactuar con bases de datos. Esta API refleja de cerca las sentencias SQL reales, permitiéndote trabajar con conceptos SQL familiares mientras aprovechas la seguridad de tipos que ofrece Kotlin.

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
}

transaction {
    // Crear la tabla
    SchemaUtils.create(Users)
    
    // Insertar datos
    val userId = Users.insert {
        it[name] = "John"
        it[email] = "john@example.com"
    } get Users.id
    
    // Consultar datos
    val user = Users.select { Users.id eq userId }.single()
    println("User: ${user[Users.name]}")
}
```

### API DAO (Data Access Object)

La API DAO proporciona un enfoque orientado a objetos para interactuar con una base de datos, similar a frameworks ORM tradicionales como Hibernate. Esta API es menos verbosa y proporciona una forma más intuitiva y centrada en Kotlin para interactuar con tu base de datos.

```kotlin
object UsersTable : IntIdTable() {
    val name = varchar("name", 50)
    val email = varchar("email", 100)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
}

transaction {
    // Crear la tabla
    SchemaUtils.create(UsersTable)
    
    // Insertar datos
    val user = User.new {
        name = "John"
        email = "john@example.com"
    }
    
    // Acceder a los datos directamente como propiedades
    println("User: ${user.name}")
}
```

## Bases de datos soportadas

Exposed actualmente soporta las siguientes bases de datos:

- H2 (versiones 2.x)
- MariaDB
- MySQL
- Oracle
- PostgreSQL (incluyendo PostgreSQL usando el driver JDBC pgjdbc-ng)
- Microsoft SQL Server
- SQLite

## Configuración de una conexión a base de datos

Para comenzar con Exposed, primero debes configurar una conexión a la base de datos usando la función `Database.connect()`:

```kotlin
// Conexión con parámetros básicos
Database.connect(
    url = "jdbc:h2:mem:test",
    driver = "org.h2.Driver",
    user = "root",
    password = ""
)

// O usando un DataSource para características avanzadas como connection pooling
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:mysql://localhost/dbname"
    driverClassName = "com.mysql.cj.jdbc.Driver"
    username = "username"
    password = "password"
    maximumPoolSize = 10
}
val dataSource = HikariDataSource(config)
Database.connect(dataSource)
```

Es importante tener en cuenta que invocar `Database.connect()` sólo configura la conexión, pero no establece inmediatamente una conexión con la base de datos. La conexión real se establecerá cuando se realice la primera operación dentro de un bloque de transacción.

## Trabajando con transacciones

Todas las operaciones de base de datos en Exposed deben realizarse dentro de un bloque de transacción:

```kotlin
transaction {
    // Aquí van las operaciones de base de datos
    SchemaUtils.create(Users)
    
    Users.insert {
        it[name] = "Alice"
        it[email] = "alice@example.com"
    }
}
```

Las transacciones en Exposed son sincrónicas y bloquean el hilo actual. Si necesitas ejecutar operaciones de manera asíncrona, puedes usar corrutinas combinadas con los helpers que proporciona Exposed:

```kotlin
// En un contexto de corrutina
launch {
    val users = newSuspendedTransaction(Dispatchers.IO) {
        Users.selectAll().toList()
    }
    // Procesa los usuarios de forma asíncrona
}
```

## Integración con Ktor

Ktor es un framework asíncrono para crear aplicaciones web en Kotlin. Puedes integrar Exposed con Ktor utilizando el siguiente patrón:

```kotlin
fun Application.configureDatabases() {
    // Instalar el plugin de bases de datos de Ktor
    install(Database) {
        // Configurar la conexión a la base de datos
    }
    
    // Instalar el plugin HikariCP para gestionar el pool de conexiones
    val dbConfig = HikariConfig().apply {
        jdbcUrl = environment.config.property("database.jdbcUrl").getString()
        driverClassName = environment.config.property("database.driverClassName").getString()
        username = environment.config.property("database.username").getString()
        password = environment.config.property("database.password").getString()
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(dbConfig)
    Database.connect(dataSource)
    
    // Inicializar las tablas
    transaction {
        SchemaUtils.create(Users)
    }
}
```

Para manejar transacciones en rutas de Ktor de forma asíncrona:

```kotlin
get("/users") {
    val users = newSuspendedTransaction(Dispatchers.IO) {
        Users.selectAll().map { 
            mapOf("id" to it[Users.id], "name" to it[Users.name], "email" to it[Users.email]) 
        }
    }
    call.respond(users)
}

post("/users") {
    val user = call.receive<UserDTO>()
    val id = newSuspendedTransaction {
        Users.insert {
            it[name] = user.name
            it[email] = user.email
        } get Users.id
    }
    call.respond(HttpStatusCode.Created, mapOf("id" to id))
}
```

## Dependencias

Para usar Exposed en tu proyecto, necesitas añadir las siguientes dependencias:

```kotlin
// build.gradle.kts
val exposedVersion = "0.46.0"

dependencies {
    // Módulos core
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    
    // Para usar la API DAO (opcional)
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    
    // Para tipos de fecha-hora de Java 8
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    
    // Driver JDBC para tu base de datos (ejemplo con H2)
    implementation("com.h2database:h2:2.2.224")
    
    // HikariCP para pooling de conexiones (recomendado para producción)
    implementation("com.zaxxer:HikariCP:5.0.1")
}
```

## Logging

Exposed utiliza SLF4J para el logging. Puedes activar el logging de consultas SQL añadiendo el siguiente código dentro de un bloque de transacción:

```kotlin
transaction {
    addLogger(StdOutSqlLogger)
    
    // Tus operaciones de base de datos
}
```

Esto imprimirá todas las consultas SQL en la consola, lo que resulta útil para desarrollo y depuración.

## Resumen

Exposed ofrece una forma elegante y tipo segura de trabajar con bases de datos relacionales en Kotlin, proporcionando dos APIs complementarias:

1. **API DSL**: Para operaciones SQL más directas y control preciso.
2. **API DAO**: Para un enfoque más orientado a objetos similar a ORMs tradicionales.

La elección entre ambas depende de tus necesidades específicas y preferencias de estilo de programación. Puedes incluso usar ambas APIs en el mismo proyecto, aprovechando las fortalezas de cada una según el caso de uso.