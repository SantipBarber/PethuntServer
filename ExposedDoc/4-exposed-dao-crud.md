# Operaciones CRUD con la API DAO de Exposed

La API DAO (Data Access Object) de Exposed proporciona un enfoque orientado a objetos para realizar operaciones CRUD (Create, Read, Update, Delete) en tu base de datos. Esta API mapea las filas de la base de datos a objetos Kotlin, ofreciendo una interfaz más orientada a objetos y natural para los desarrolladores de Kotlin.

## Configuración previa

Para los ejemplos en este documento, asumiremos las siguientes definiciones de tablas y entidades:

```kotlin
// Definición de tablas (IdTable)
object UsersTable : IntIdTable() {
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    val age = integer("age").nullable()
    val isActive = bool("is_active").default(true)
}

object PostsTable : IntIdTable() {
    val userId = reference("user_id", UsersTable)
    val title = varchar("title", 100)
    val content = text("content")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

// Definición de entidades
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    var age by UsersTable.age
    var isActive by UsersTable.isActive
    
    // Transformación para almacenar siempre en minúsculas
    var normalizedEmail by UsersTable.email.transform(
        wrap = { it.lowercase() },
        unwrap = { it }
    )
    
    // Transformación con memo para evitar cálculos repetidos
    var displayName by UsersTable.name.memoizedTransform(
        wrap = { "👤 $it" },
        unwrap = { it.removePrefix("👤 ") }
    )
}id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    var age by UsersTable.age
    var isActive by UsersTable.isActive
    
    // Relación one-to-many con posts
    val posts by Post referrersOn PostsTable.userId
}

class Post(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Post>(PostsTable)
    
    var user by User referencedOn PostsTable.userId
    var title by PostsTable.title
    var content by PostsTable.content
    var createdAt by PostsTable.createdAt
}
```

## Create (Crear)

### Crear una nueva entidad

Para crear una nueva entidad en la base de datos, utiliza el método `.new`:

```kotlin
transaction {
    val user = User.new {
        name = "John Doe"
        email = "john@example.com"
        age = 30
    }
    
    // El ID se asigna automáticamente y está disponible inmediatamente
    println("Usuario creado con ID: ${user.id.value}")
}
```

### Crear entidad con ID específico

Puedes especificar un ID al crear una entidad (si la tabla lo permite):

```kotlin
transaction {
    val user = User.new(5) {  // Asignar ID = 5
        name = "Custom ID User"
        email = "custom@example.com"
        age = 25
    }
}
```

### Crear entidades relacionadas

Puedes crear entidades relacionadas de forma natural:

```kotlin
transaction {
    // Primero creamos un usuario
    val user = User.new {
        name = "Blog Author"
        email = "author@example.com"
    }
    
    // Luego creamos un post que pertenece a ese usuario
    val post = Post.new {
        this.user = user  // Asigna la referencia directamente
        title = "Mi primer post"
        content = "Contenido del post..."
    }
}
```

## Read (Leer)

### Buscar por ID

Para encontrar una entidad por su ID:

```kotlin
transaction {
    // Devuelve la entidad o lanza una excepción si no existe
    val user = User.findById(1)
    
    // Devuelve la entidad o null si no existe
    val userOrNull = User.findById(999)
}
```

### Buscar todos

Para obtener todas las entidades:

```kotlin
transaction {
    val allUsers = User.all().toList()
    
    // Iterar por los resultados
    allUsers.forEach {
        println("Usuario: ${it.name}, Email: ${it.email}")
    }
}
```

### Buscar con condiciones

Para buscar entidades con condiciones específicas, utiliza el método `.find`:

```kotlin
transaction {
    // Buscar usuarios activos
    val activeUsers = User.find { UsersTable.isActive eq true }
    
    // Buscar usuarios por edad
    val adultUsers = User.find { UsersTable.age greaterEq 18 }
    
    // Buscar con condiciones compuestas
    val specificUsers = User.find {
        (UsersTable.email like "%@gmail.com") and 
        (UsersTable.age greater 21)
    }
}
```

### Buscar un único resultado

Cuando esperas un solo resultado:

```kotlin
transaction {
    // Buscar por email (suponiendo que email es único)
    val user = User.find { UsersTable.email eq "john@example.com" }.singleOrNull()
    
    if (user != null) {
        println("Usuario encontrado: ${user.name}")
    } else {
        println("Usuario no encontrado")
    }
}
```

### Navegación por relaciones

Una de las ventajas de la API DAO es la capacidad de navegar por relaciones de forma intuitiva:

```kotlin
transaction {
    // Obtener un usuario
    val user = User.findById(1)
    
    // Obtener todos los posts de ese usuario
    val userPosts = user?.posts?.toList() ?: emptyList()
    
    // Acceder a los datos de los posts
    userPosts.forEach {
        println("Post: ${it.title}")
        println("Contenido: ${it.content}")
        println("Creado: ${it.createdAt}")
    }
    
    // También puedes ir en la otra dirección
    val post = Post.findById(1)
    val author = post?.user
    println("El autor de '${post?.title}' es ${author?.name}")
}
```

### Ordenar resultados

Para ordenar los resultados:

```kotlin
transaction {
    // Ordenar por nombre (ascendente)
    val orderedUsers = User.all().orderBy(UsersTable.name to SortOrder.ASC)
    
    // Ordenar por múltiples campos
    val complexOrderedUsers = User.all().orderBy(
        UsersTable.age to SortOrder.DESC_NULLS_LAST,
        UsersTable.name to SortOrder.ASC
    )
}
```

### Limitar y paginar resultados

Para limitar o paginar resultados:

```kotlin
transaction {
    // Obtener los primeros 10 usuarios
    val first10Users = User.all().limit(10)
    
    // Paginación
    val pageSize = 20
    val page = 3
    val paginatedUsers = User.all().limit(pageSize, offset = (page - 1L) * pageSize)
}
```

## Update (Actualizar)

### Actualizar una entidad

En la API DAO, la actualización es tan simple como modificar las propiedades del objeto:

```kotlin
transaction {
    val user = User.findById(1)
    
    // Actualizar propiedades
    user?.apply {
        name = "Updated Name"
        email = "updated@example.com"
        age = 31
    }
    
    // No se necesita llamar a un método de actualización explícito,
    // los cambios se guardan automáticamente al final de la transacción
}
```

### Actualizar con el patrón builder

También puedes usar un patrón similar al de la creación:

```kotlin
transaction {
    User.findById(1)?.apply {
        name = "New Name"
        isActive = false
    }
}
```

### Buscar y actualizar en una operación

Para buscar y actualizar en una operación:

```kotlin
transaction {
    User.findByIdAndUpdate(1) {
        it.name = "Updated via function"
        it.age = 40
    }
    
    // También funciona con consultas
    User.findSingleByAndUpdate({ UsersTable.email eq "john@example.com" }) {
        it.isActive = false
    }
}
```

## Delete (Eliminar)

### Eliminar una entidad

Para eliminar una entidad existente:

```kotlin
transaction {
    val user = User.findById(1)
    user?.delete()
}
```

### Eliminar múltiples entidades

Para eliminar múltiples entidades basadas en una condición:

```kotlin
transaction {
    // Encontrar usuarios inactivos
    val inactiveUsers = User.find { UsersTable.isActive eq false }
    
    // Eliminarlos uno por uno
    inactiveUsers.forEach { it.delete() }
}
```

Exposed no proporciona directamente un método para eliminar en masa con la API DAO, pero puedes combinarlo con la API DSL:

```kotlin
transaction {
    // Eliminar todos los usuarios inactivos utilizando la API DSL
    UsersTable.deleteWhere { UsersTable.isActive eq false }
}
```

## Relaciones

La API DAO de Exposed maneja relaciones de manera elegante. Veamos los diferentes tipos de relaciones:

### One-to-Many (Uno a Muchos)

Ya definimos la relación one-to-many entre User y Post usando `referrersOn`:

```kotlin
// En la clase User
val posts by Post referrersOn PostsTable.userId
```

Esto permite acceder a todos los posts de un usuario:

```kotlin
transaction {
    val user = User.findById(1)
    val userPosts = user?.posts?.toList() ?: emptyList()
    
    println("${user?.name} tiene ${userPosts.size} posts")
}
```

### Many-to-One (Muchos a Uno)

La relación inversa, de Post a User, se define con `referencedOn`:

```kotlin
// En la clase Post
var user by User referencedOn PostsTable.userId
```

Acceso:

```kotlin
transaction {
    val post = Post.findById(1)
    val author = post?.user
    
    println("El post '${post?.title}' fue escrito por ${author?.name}")
}
```

### Many-to-Many (Muchos a Muchos)

Para una relación many-to-many, necesitas una tabla de unión:

```kotlin
// Tabla de unión
object UserTagsTable : Table() {
    val user = reference("user_id", UsersTable)
    val tag = reference("tag_id", TagsTable)
    
    override val primaryKey = PrimaryKey(user, tag)
}

// Tabla de tags
object TagsTable : IntIdTable() {
    val name = varchar("name", 50)
}

// Entidad Tag
class Tag(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Tag>(TagsTable)
    
    var name by TagsTable.name
    var users by User via UserTagsTable
}

// Añadir propiedad para la relación en User
class User(id: EntityID<Int>) : IntEntity(id) {
    // ... otras propiedades ...
    
    var tags by Tag via UserTagsTable
}
```

Uso:

```kotlin
transaction {
    // Crear algunos tags
    val tag1 = Tag.new { name = "kotlin" }
    val tag2 = Tag.new { name = "programming" }
    
    // Asignar tags a un usuario
    val user = User.findById(1)
    user?.tags = SizedCollection(listOf(tag1, tag2))
    
    // Consultar usuarios con ciertos tags
    val kotlinUsers = Tag.find { TagsTable.name eq "kotlin" }
        .flatMap { it.users }
        .toSet()
    
    println("Usuarios con tag 'kotlin': ${kotlinUsers.size}")
}
```

### Relación Opcional

Para relaciones opcionales, usa `optionalReferencedOn`:

```kotlin
// Tabla con referencia opcional
object ProfilesTable : IntIdTable() {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val bio = text("bio").nullable()
    val supervisorId = optReference("supervisor_id", UsersTable)
}

class Profile(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Profile>(ProfilesTable)
    
    var user by User referencedOn ProfilesTable.userId
    var bio by ProfilesTable.bio
    var supervisor by User optionalReferencedOn ProfilesTable.supervisorId
}

// Añadir la relación a User
class User(id: EntityID<Int>) : IntEntity(id) {
    // ... otras propiedades ...
    
    val profile by Profile optionalBackReferencedOn ProfilesTable.userId
}
```

Uso:

```kotlin
transaction {
    val user = User.findById(1)
    val profile = Profile.new {
        this.user = user!!
        bio = "Una breve biografía..."
    }
    
    // Asignar un supervisor (opcional)
    val supervisor = User.findById(2)
    if (supervisor != null) {
        profile.supervisor = supervisor
    }
    
    // Acceder a la relación opcional
    val profileSupervisor = profile.supervisor
    if (profileSupervisor != null) {
        println("El perfil está supervisado por: ${profileSupervisor.name}")
    } else {
        println("El perfil no tiene supervisor")
    }
}
```

## Carga Eager (Eager Loading)

Por defecto, Exposed carga las relaciones de forma perezosa (lazy loading). Para cargar relaciones de forma eager y evitar el problema N+1, usa el método `.load()`:

```kotlin
transaction {
    // Cargar un usuario y sus posts en una sola consulta
    val user = User.findById(1)?.load(User::posts)
    
    // Ahora puedes acceder a user.posts sin consultas adicionales
    user?.posts?.forEach {
        println("Post: ${it.title}")
    }
    
    // Cargar múltiples usuarios con sus posts
    val users = User.all().with(User::posts)
    
    users.forEach { user ->
        println("Usuario: ${user.name}")
        println("Número de posts: ${user.posts.count()}")
    }
}
```

## Transformaciones de Columnas

Puedes transformar los valores de columnas al mapearlos a propiedades de entidad, lo que permite convertir datos entre la representación de la base de datos y el modelo de dominio:

```kotlin
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    var age by UsersTable.age
    var isActive by UsersTable.isActive
    
    // Transformación para almacenar siempre en minúsculas
    var normalizedEmail by UsersTable.email.transform(
        wrap = { it.lowercase() },
        unwrap = { it }
    )
    
    // Transformación con memo para evitar cálculos repetidos
    var displayName by UsersTable.name.memoizedTransform(
        wrap = { "👤 $it" },
        unwrap = { it.removePrefix("👤 ") }
    )
}
```

También puedes crear transformadores reutilizables:

```kotlin
// Transformador personalizado
class UUIDStringTransformer : ColumnTransformer<String, UUID>() {
    override fun wrap(value: String): UUID = UUID.fromString(value)
    override fun unwrap(value: UUID): String = value.toString()
}

// Uso del transformador
class ApiKey(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ApiKey>(ApiKeysTable)
    
    var keyString by ApiKeysTable.keyValue
    var keyUUID by ApiKeysTable.keyValue.transform(UUIDStringTransformer())
}
```

## Callbacks del ciclo de vida

La API DAO de Exposed proporciona métodos para interceptar eventos del ciclo de vida de las entidades:

```kotlin
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable) {
        // Ejecuta código antes de insertar
        override fun onCreate(id: EntityID<Int>, row: ResultRow): User {
            println("Creando usuario con ID: $id")
            return super.onCreate(id, row)
        }
        
        // Ejecuta código antes de eliminar
        override fun onDelete(entity: Entity<Int>, super: Entity<Int>.() -> Unit) {
            println("Eliminando usuario: ${entity.id}")
            super(entity)
        }
    }
    
    // Resto de propiedades...
    
    // Personalizar la eliminación
    override fun delete() {
        println("Eliminando usuario $name con ID: $id")
        super.delete()
    }
}
```

Exposed también proporciona EntityHooks para escuchar cambios en entidades:

```kotlin
transaction {
    EntityHook.subscribe { action ->
        when (action.changeType) {
            EntityChangeType.Created -> println("Entidad creada: ${action.toEntity(User)}")
            EntityChangeType.Updated -> println("Entidad actualizada: ${action.toEntity(User)}")
            EntityChangeType.Removed -> println("Entidad eliminada: ${action.id}")
        }
    }
}
```

## Entidades inmutables

Exposed también soporta entidades inmutables a través de ImmutableEntityClass:

```kotlin
object LogEntriesTable : IntIdTable() {
    val message = text("message")
    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}

class LogEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : ImmutableEntityClass<LogEntry>(LogEntriesTable)
    
    val message by LogEntriesTable.message
    val timestamp by LogEntriesTable.timestamp
}
```

Con entidades inmutables, solo puedes leer sus propiedades, no modificarlas:

```kotlin
transaction {
    // Crear entrada mediante la API DSL
    val entryId = LogEntriesTable.insertAndGetId {
        it[message] = "Sistema iniciado"
    }
    
    // Leer la entrada
    val logEntry = LogEntry.findById(entryId)
    println("${logEntry?.timestamp}: ${logEntry?.message}")
    
    // No se puede modificar
    // logEntry?.message = "Nuevo mensaje" // Error de compilación
}
```

## Uso con Ktor

Veamos cómo usar la API DAO de Exposed en una aplicación Ktor:

```kotlin
// Configuración de la ruta para usuarios
fun Route.userRoutes() {
    route("/users") {
        // Obtener todos los usuarios
        get {
            val users = newSuspendedTransaction {
                User.all().map {
                    mapOf(
                        "id" to it.id.value,
                        "name" to it.name,
                        "email" to it.email,
                        "age" to it.age,
                        "isActive" to it.isActive
                    )
                }
            }
            call.respond(users)
        }
        
        // Obtener un usuario por ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@get
            }
            
            val user = newSuspendedTransaction {
                User.findById(id)?.let {
                    mapOf(
                        "id" to it.id.value,
                        "name" to it.name,
                        "email" to it.email,
                        "age" to it.age,
                        "isActive" to it.isActive,
                        "postsCount" to it.posts.count()
                    )
                }
            }
            
            if (user != null) {
                call.respond(user)
            } else {
                call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
            }
        }
        
        // Crear un nuevo usuario
        post {
            val userDto = call.receive<UserDto>()
            
            val newUser = newSuspendedTransaction {
                User.new {
                    name = userDto.name
                    email = userDto.email
                    age = userDto.age
                }
            }
            
            call.respond(
                HttpStatusCode.Created, 
                mapOf("id" to newUser.id.value, "message" to "Usuario creado correctamente")
            )
        }
        
        // Actualizar un usuario
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@put
            }
            
            val userDto = call.receive<UserDto>()
            
            val updated = newSuspendedTransaction {
                User.findById(id)?.apply {
                    name = userDto.name
                    email = userDto.email
                    age = userDto.age
                } != null
            }
            
            if (updated) {
                call.respond(HttpStatusCode.OK, "Usuario actualizado correctamente")
            } else {
                call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
            }
        }
        
        // Eliminar un usuario
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
                return@delete
            }
            
            val deleted = newSuspendedTransaction {
                User.findById(id)?.apply {
                    delete()
                } != null
            }
            
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Usuario eliminado correctamente")
            } else {
                call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
            }
        }
    }
}
```

## Consultas avanzadas con DAO y DSL

A veces necesitas combinar la flexibilidad de DSL con la conveniencia de DAO:

```kotlin
transaction {
    // Consulta compleja con DSL
    val query = (UsersTable innerJoin PostsTable)
        .slice(UsersTable.id, UsersTable.name, PostsTable.id.count())
        .select { UsersTable.isActive eq true }
        .groupBy(UsersTable.id, UsersTable.name)
        .having { PostsTable.id.count() greater 5 }
    
    // Convertir los resultados a entidades DAO
    val activeUsersWithManyPosts = User.wrapRows(query).toList()
    
    // Ahora puedes usar estos objetos User normalmente
    activeUsersWithManyPosts.forEach {
        println("${it.name} tiene ${it.posts.count()} posts")
    }
}
```

## Resumen

La API DAO de Exposed ofrece una forma orientada a objetos y natural de trabajar con bases de datos en Kotlin:

* Create: EntityClass.new { }
* Read: EntityClass.findById(), EntityClass.all(), EntityClass.find { }
* Update: Simplemente modificando propiedades de objetos
* Delete: entity.delete()

Beneficios principales:

* Mapeo directo entre filas de la base de datos y objetos Kotlin
* Manejo intuitivo de relaciones (one-to-many, many-to-many, etc.)
* Navegación natural a través de las relaciones entre objetos
* Transformaciones de datos entre la base de datos y el modelo de dominio

La API DAO es ideal cuando prefieres un enfoque más orientado a objetos y menos centrado en SQL, aunque siempre puedes combinarla con la API DSL cuando necesites mayor flexibilidad.