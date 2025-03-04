# Relaciones entre tablas en Exposed

Las relaciones entre tablas son fundamentales en cualquier esquema de base de datos relacional. Exposed proporciona mecanismos para modelar estas relaciones tanto en la API DSL como en la API DAO. Este documento explica cómo definir y trabajar con diferentes tipos de relaciones.

## Tipos de relaciones

Las bases de datos relacionales soportan varios tipos de relaciones:

1. **One-to-One (Uno a Uno)**: Un registro en la tabla A está relacionado con exactamente un registro en la tabla B.
2. **One-to-Many (Uno a Muchos)**: Un registro en la tabla A está relacionado con varios registros en la tabla B.
3. **Many-to-One (Muchos a Uno)**: Varios registros en la tabla A están relacionados con un registro en la tabla B (es la inversa de One-to-Many).
4. **Many-to-Many (Muchos a Muchos)**: Varios registros en la tabla A están relacionados con varios registros en la tabla B.

## Definiendo relaciones con la API DSL

En la API DSL, las relaciones se establecen principalmente mediante columnas de referencia (claves foráneas).

### One-to-Many y Many-to-One

Estas relaciones se definen mediante claves foráneas:

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    
    override val primaryKey = PrimaryKey(id)
}

object Posts : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 100)
    val content = text("content")
    // Relación Many-to-One (muchos posts pertenecen a un usuario)
    val userId = reference("user_id", Users)
    
    override val primaryKey = PrimaryKey(id)
}
```

En este ejemplo:
- La columna `userId` en la tabla `Posts` es una clave foránea que hace referencia a la columna `id` de la tabla `Users`.
- Esto establece una relación One-to-Many desde `Users` a `Posts` (un usuario tiene muchos posts) y, al mismo tiempo, una relación Many-to-One desde `Posts` a `Users` (muchos posts pertenecen a un usuario).

### Referencia opcional

Si la relación es opcional (permite NULL), puedes usar `optReference`:

```kotlin
object Employees : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    // Referencia opcional - un empleado puede no tener un manager
    val managerId = optReference("manager_id", Employees)
    
    override val primaryKey = PrimaryKey(id)
}
```

### Many-to-Many

Las relaciones Many-to-Many requieren una tabla de unión:

```kotlin
object Students : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    
    override val primaryKey = PrimaryKey(id)
}

object Courses : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    
    override val primaryKey = PrimaryKey(id)
}

// Tabla de unión para la relación Many-to-Many
object StudentCourses : Table() {
    val studentId = reference("student_id", Students)
    val courseId = reference("course_id", Courses)
    
    // Definir una clave primaria compuesta
    override val primaryKey = PrimaryKey(studentId, courseId)
}
```

## Consultando relaciones con la API DSL

### Consultar relaciones One-to-Many

```kotlin
transaction {
    // Encontrar todos los posts de un usuario específico
    val postsFromUser = Posts.select { Posts.userId eq 1 }.toList()
    
    // Join explícito para obtener usuario con sus posts
    val userWithPosts = (Users innerJoin Posts)
        .select { Users.id eq 1 }
        .map { row ->
            mapOf(
                "userId" to row[Users.id],
                "userName" to row[Users.name],
                "postId" to row[Posts.id],
                "postTitle" to row[Posts.title]
            )
        }
}
```

### Consultar relaciones Many-to-Many

```kotlin
transaction {
    // Encontrar todos los cursos de un estudiante
    val coursesForStudent = (Courses innerJoin StudentCourses)
        .select { StudentCourses.studentId eq 1 }
        .map { it[Courses.name] }
    
    // Encontrar todos los estudiantes de un curso
    val studentsInCourse = (Students innerJoin StudentCourses)
        .select { StudentCourses.courseId eq 1 }
        .map { it[Students.name] }
    
    // Join triple para datos más detallados
    val studentCourseDetails = (Students innerJoin StudentCourses innerJoin Courses)
        .select { Students.id eq 1 }
        .map { row ->
            mapOf(
                "studentName" to row[Students.name],
                "courseName" to row[Courses.name]
            )
        }
}
```

## Modificando relaciones con la API DSL

### Añadir relaciones

```kotlin
transaction {
    // Añadir un post a un usuario
    Posts.insert {
        it[title] = "Nuevo post"
        it[content] = "Contenido del post"
        it[userId] = 1 // Relacionar con el usuario de ID 1
    }
    
    // Añadir una relación Many-to-Many (inscribir un estudiante en un curso)
    StudentCourses.insert {
        it[studentId] = 1
        it[courseId] = 2
    }
}
```

### Modificar relaciones

```kotlin
transaction {
    // Cambiar el usuario de un post
    Posts.update({ Posts.id eq 1 }) {
        it[userId] = 2 // Ahora pertenece al usuario con ID 2
    }
}
```

### Eliminar relaciones

```kotlin
transaction {
    // Eliminar una relación Many-to-Many (dar de baja a un estudiante de un curso)
    StudentCourses.deleteWhere {
        (StudentCourses.studentId eq 1) and (StudentCourses.courseId eq 2)
    }
}
```

## Definiendo relaciones con la API DAO

En la API DAO, las relaciones se definen en las clases de entidad.

### Definiciones de tabla

Primero, definimos las tablas (similar a la API DSL, pero usando clases IdTable):

```kotlin
object UsersTable : IntIdTable() {
    val name = varchar("name", 50)
    val email = varchar("email", 100)
}

object PostsTable : IntIdTable() {
    val title = varchar("title", 100)
    val content = text("content")
    val userId = reference("user_id", UsersTable)
}

object TagsTable : IntIdTable() {
    val name = varchar("name", 50)
}

object PostTagsTable : Table() {
    val postId = reference("post_id", PostsTable)
    val tagId = reference("tag_id", TagsTable)
    
    override val primaryKey = PrimaryKey(postId, tagId)
}
```

### One-to-Many y Many-to-One

```kotlin
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    
    // Relación One-to-Many (un usuario tiene muchos posts)
    val posts by Post referrersOn PostsTable.userId
}

class Post(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Post>(PostsTable)
    
    var title by PostsTable.title
    var content by PostsTable.content
    
    // Relación Many-to-One (muchos posts pertenecen a un usuario)
    var user by User referencedOn PostsTable.userId
}
```

### One-to-One

```kotlin
object UserProfilesTable : IntIdTable() {
    val bio = text("bio").nullable()
    val userId = reference("user_id", UsersTable).uniqueIndex()
}

class UserProfile(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserProfile>(UserProfilesTable)
    
    var bio by UserProfilesTable.bio
    var user by User referencedOn UserProfilesTable.userId
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
    
    // Relación One-to-One (un usuario tiene un solo perfil)
    val profile by UserProfile optionalBackReferencedOn UserProfilesTable.userId
}
```

La palabra clave `optionalBackReferencedOn` se usa porque el perfil es opcional (puede no existir).

### Many-to-Many

```kotlin
class Tag(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Tag>(TagsTable)
    
    var name by TagsTable.name
    
    // Relación Many-to-Many (muchos tags están en muchos posts)
    var posts by Post via PostTagsTable
}

class Post(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Post>(PostsTable)
    
    var title by PostsTable.title
    var content by PostsTable.content
    var user by User referencedOn PostsTable.userId
    
    // Relación Many-to-Many (muchos posts tienen muchos tags)
    var tags by Tag via PostTagsTable
}
```

La palabra clave `via` especifica la tabla de unión que conecta las dos entidades.

## Relaciones autorreferenciales

Las relaciones autorreferenciales se dan cuando una tabla hace referencia a sí misma:

```kotlin
object EmployeesTable : IntIdTable() {
    val name = varchar("name", 50)
    val managerId = optReference("manager_id", EmployeesTable)
}

class Employee(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Employee>(EmployeesTable)
    
    var name by EmployeesTable.name
    var manager by Employee optionalReferencedOn EmployeesTable.managerId
    val subordinates by Employee referrersOn EmployeesTable.managerId
}
```

En este ejemplo:
- `manager` es una referencia al jefe de un empleado (puede ser null)
- `subordinates` es una colección de todos los empleados que tienen a este empleado como jefe

## Relaciones con clave primaria compuesta

Cuando una tabla tiene una clave primaria compuesta, puedes definir relaciones a ella:

```kotlin
object OrderItemsTable : Table() {
    val orderId = reference("order_id", OrdersTable)
    val productId = reference("product_id", ProductsTable)
    val quantity = integer("quantity")
    
    override val primaryKey = PrimaryKey(orderId, productId)
}

object InventoryMovementsTable : IntIdTable() {
    val orderItemOrderId = reference("order_item_order_id", OrderItemsTable.orderId)
    val orderItemProductId = reference("order_item_product_id", OrderItemsTable.productId)
    val quantity = integer("quantity")
    
    init {
        foreignKey(
            orderItemOrderId, orderItemProductId,
            target = OrderItemsTable.primaryKey
        )
    }
}
```

## Trabajando con relaciones en la API DAO

### Acceder a relaciones

```kotlin
transaction {
    // Obtener un usuario
    val user = User.findById(1)
    
    // Acceder a sus posts (One-to-Many)
    user?.posts?.forEach { post ->
        println("Título: ${post.title}")
    }
    
    // Obtener un post
    val post = Post.findById(1)
    
    // Acceder a su autor (Many-to-One)
    val author = post?.user
    println("Autor: ${author?.name}")
    
    // Acceder a sus tags (Many-to-Many)
    post?.tags?.forEach { tag ->
        println("Tag: ${tag.name}")
    }
}
```

### Modificar relaciones

#### One-to-Many y Many-to-One

```kotlin
transaction {
    // Obtener entidades
    val user = User.findById(1)
    val post = Post.findById(1)
    
    // Cambiar la relación Many-to-One
    if (post != null && user != null) {
        post.user = user  // Asignar un nuevo usuario al post
    }
}
```

#### Many-to-Many

Para relaciones Many-to-Many, usamos `SizedCollection`:

```kotlin
transaction {
    val post = Post.findById(1)
    val tags = Tag.find { TagsTable.name inList listOf("kotlin", "exposed") }.toList()
    
    // Asignar los tags al post (reemplaza cualquier tag existente)
    post?.tags = SizedCollection(tags)
    
    // Añadir un nuevo tag a los existentes
    val currentTags = post?.tags?.toMutableList() ?: mutableListOf()
    val newTag = Tag.new { name = "database" }
    currentTags.add(newTag)
    post?.tags = SizedCollection(currentTags)
    
    // Eliminar todos los tags
    post?.tags = SizedCollection(emptyList())
}
```

## Eager Loading (Carga Anticipada)

Por defecto, Exposed carga las relaciones de forma perezosa (lazy), lo que puede llevar al problema "N+1". Para evitarlo, puedes usar Eager Loading:

```kotlin
transaction {
    // Problema N+1: Por cada usuario, se hace una consulta para cargar sus posts
    val users = User.all().toList()
    users.forEach { user ->
        println("${user.name} tiene ${user.posts.count()} posts")  // Consulta adicional por cada usuario
    }
    
    // Solución: Eager Loading
    val usersWithPosts = User.all().with(User::posts)
    usersWithPosts.forEach { user ->
        println("${user.name} tiene ${user.posts.count()} posts")  // No se hacen consultas adicionales
    }
    
    // Cargar relaciones anidadas
    val usersWithPostsAndTags = User.all()
        .with(User::posts, Post::tags)
    
    // También se puede hacer para una sola entidad
    val user = User.findById(1)?.load(User::posts)
}
```

## Ordenar relaciones

Puedes especificar el orden de las entidades relacionadas:

```kotlin
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    
    // Posts ordenados por título ascendente
    val postsByTitle by Post referrersOn PostsTable.userId orderBy PostsTable.title
    
    // Posts ordenados por fecha de creación descendente
    val postsByDate by Post referrersOn PostsTable.userId orderBy (PostsTable.createdAt to SortOrder.DESC)
    
    // Múltiples criterios de ordenación
    val postsSorted by Post referrersOn PostsTable.userId orderBy listOf(
        PostsTable.createdAt to SortOrder.DESC,
        PostsTable.title to SortOrder.ASC
    )
}
```

## Filtrar relaciones

Aunque Exposed no proporciona un mecanismo directo para filtrar relaciones como algunos ORM, puedes lograr este comportamiento:

```kotlin
transaction {
    // Definir la consulta filtrada
    val recentPosts = Post.find {
        (PostsTable.userId eq 1) and 
        (PostsTable.createdAt greater LocalDateTime.now().minusDays(7))
    }
    
    // Usarla cuando sea necesario
    val user = User.findById(1)
    println("${user?.name} tiene ${recentPosts.count()} posts recientes")
}
```

## Definir relaciones en tiempo de ejecución

En algunos casos, podrías necesitar definir relaciones dinámicamente:

```kotlin
class DynamicEntity(id: EntityID<Int>, table: IntIdTable) : IntEntity(id) {
    companion object : IntEntityClass<DynamicEntity>(DummyTable)
    
    private val properties = mutableMapOf<String, Any?>()
    
    operator fun <T> get(column: Column<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return properties[column.name] as T?
    }
    
    operator fun <T> set(column: Column<T>, value: T?) {
        properties[column.name] = value
    }
    
    fun <T : Entity<Int>> setReference(column: Column<EntityID<Int>>, entity: T?) {
        properties[column.name] = entity?.id
    }
    
    fun <T : Entity<Int>> getReference(column: Column<EntityID<Int>>, entityClass: EntityClass<Int, T>): T? {
        val entityId = properties[column.name] as EntityID<Int>?
        return entityId?.let { entityClass.findById(it) }
    }
}
```

## Escenarios avanzados

### Relaciones polimórficas

Exposed no soporta nativamente relaciones polimórficas, pero puedes implementarlas manualmente:

```kotlin
object CommentsTable : IntIdTable() {
    val content = text("content")
    val targetType = varchar("target_type", 50)
    val targetId = integer("target_id")
    
    init {
        index(false, targetType, targetId)
    }
}

class Comment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Comment>(CommentsTable) {
        // Encontrar comentarios para una entidad específica
        fun findForTarget(targetType: String, targetId: Int): SizedIterable<Comment> {
            return find {
                (CommentsTable.targetType eq targetType) and 
                (CommentsTable.targetId eq targetId)
            }
        }
    }
    
    var content by CommentsTable.content
    var targetType by CommentsTable.targetType
    var targetId by CommentsTable.targetId
    
    // Establecer la entidad objetivo
    fun setTarget(entity: IntEntity) {
        targetType = entity.javaClass.simpleName
        targetId = entity.id.value
    }
}

// Uso
transaction {
    val post = Post.findById(1)
    val user = User.findById(1)
    
    // Crear comentarios para diferentes tipos de entidades
    val postComment = Comment.new {
        content = "Gran post!"
        setTarget(post!!)
    }
    
    val userComment = Comment.new {
        content = "Gran usuario!"
        setTarget(user!!)
    }
    
    // Obtener comentarios
    val postComments = Comment.findForTarget("Post", post!!.id.value)
    val userComments = Comment.findForTarget("User", user!!.id.value)
}
```

### Relaciones a través de múltiples tablas

Para relaciones complejas que atraviesan múltiples tablas:

```kotlin
transaction {
    // Encontrar todos los tags usados por un usuario específico
    val userTags = (TagsTable innerJoin PostTagsTable innerJoin PostsTable)
        .select { PostsTable.userId eq 1 }
        .withDistinct()
        .map { it[TagsTable.name] }
}
```

Con la API DAO:

```kotlin
transaction {
    val user = User.findById(1)
    
    // Obtener todos los tags de todos los posts del usuario
    val userTags = user?.posts?.flatMap { it.tags }?.distinctBy { it.id }
}
```

## Resumen

Exposed proporciona mecanismos flexibles para definir y trabajar con relaciones:

1. **API DSL**:
   - Usa `reference` y `optReference` para definir claves foráneas
   - Utiliza joins para consultar relaciones
   - Manipula relaciones a través de operaciones CRUD en las tablas

2. **API DAO**:
   - Define relaciones con propiedades y delegados como `referencedOn`, `referrersOn`, etc.
   - Accede a relaciones como propiedades de objetos
   - Modifica relaciones asignando nuevas entidades o colecciones
   - Usa `with()` y `load()` para Eager Loading y evitar el problema N+1

La elección entre DSL y DAO depende de tus preferencias y necesidades:
- **DSL**: Mayor control, más explícito, similar a SQL
- **DAO**: Más orientado a objetos, más natural en código Kotlin

En proyectos reales, a menudo se usan ambas APIs según sea necesario para cada caso de uso.