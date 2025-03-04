# Casos de uso comunes con Exposed

Este documento presenta soluciones para escenarios frecuentes al desarrollar aplicaciones con Exposed.

## Autenticación y autorización

### Sistema de autenticación básico

```kotlin
// Definición de tablas
object Users : IntIdTable() {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val active = bool("active").default(true)
}

object Roles : IntIdTable() {
    val name = varchar("name", 20).uniqueIndex()
}

object UserRoles : Table() {
    val user = reference("user_id", Users)
    val role = reference("role_id", Roles)
    
    override val primaryKey = PrimaryKey(user, role)
}

// Entidades
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)
    
    var username by Users.username
    var passwordHash by Users.passwordHash
    var active by Users.active
    var roles by Role via UserRoles
}

class Role(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Role>(Roles)
    
    var name by Roles.name
    var users by User via UserRoles
}

// Servicio de autenticación
class AuthService {
    fun authenticate(username: String, password: String): User? = transaction {
        User.find { Users.username eq username and Users.active eq true }
            .singleOrNull()
            ?.takeIf { BCrypt.checkpw(password, it.passwordHash) }
    }
    
    fun hasRole(user: User, roleName: String): Boolean = transaction {
        user.roles.any { it.name == roleName }
    }
    
    fun registerUser(username: String, password: String): User = transaction {
        User.new {
            this.username = username
            this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        }
    }
}
```

### Tokens JWT con Exposed

```kotlin
object RefreshTokens : IntIdTable() {
    val token = varchar("token", 255).uniqueIndex()
    val userId = reference("user_id", Users)
    val expiresAt = datetime("expires_at")
    val revoked = bool("revoked").default(false)
}

class TokenService(private val secret: String) {
    fun generateAccessToken(user: User): String = transaction {
        val roles = user.roles.map { it.name }
        
        JWT.create()
            .withSubject(user.id.value.toString())
            .withClaim("username", user.username)
            .withArrayClaim("roles", roles.toTypedArray())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hora
            .sign(Algorithm.HMAC256(secret))
    }
    
    fun generateRefreshToken(user: User): String = transaction {
        val token = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusDays(30)
        
        RefreshTokens.insert {
            it[RefreshTokens.token] = token
            it[userId] = user.id
            it[RefreshTokens.expiresAt] = expiresAt
        }
        
        token
    }
}
```

## Paginación

### Paginación básica

```kotlin
fun getPagedUsers(page: Int, pageSize: Int): Pair<List<User>, Long> = transaction {
    val totalUsers = User.count()
    val users = User.all()
        .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
        .toList()
    
    Pair(users, totalUsers)
}
```

### Paginación con sorting

```kotlin
fun getPagedUsers(
    page: Int,
    pageSize: Int,
    sortField: String = "id",
    sortOrder: SortOrder = SortOrder.ASC
): Pair<List<UserDto>, Long> = transaction {
    // Mapeo de nombres de campo a columnas
    val sortColumn = when (sortField) {
        "username" -> Users.username
        "createdAt" -> Users.createdAt
        else -> Users.id
    }
    
    val query = User.all()
    
    val totalUsers = query.count()
    val users = query
        .orderBy(sortColumn to sortOrder)
        .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
        .map { it.toDto() }
    
    Pair(users, totalUsers)
}
```

### Cursor-based paginación

```kotlin
fun getUsersAfter(cursor: Int?, limit: Int = 20): Pair<List<UserDto>, Int?> = transaction {
    val query = if (cursor != null) {
        User.find { Users.id greater cursor }
    } else {
        User.all()
    }
    
    val users = query
        .orderBy(Users.id to SortOrder.ASC)
        .limit(limit + 1) // Request one more than needed
        .toList()
    
    val hasMore = users.size > limit
    val result = if (hasMore) users.dropLast(1) else users
    val nextCursor = if (hasMore) result.last().id.value else null
    
    Pair(result.map { it.toDto() }, nextCursor)
}
```

## Búsqueda y filtrado

### Búsqueda simple

```kotlin
fun searchUsers(query: String): List<User> = transaction {
    User.find {
        (Users.username like "%$query%") or
        (Users.email like "%$query%")
    }.toList()
}
```

### Filtrado dinámico

```kotlin
fun findUsers(
    username: String? = null,
    email: String? = null,
    active: Boolean? = null,
    roleId: Int? = null
): List<User> = transaction {
    // Construir condición base
    var op: Op<Boolean> = Op.TRUE
    
    // Añadir condiciones según parámetros
    username?.let { op = op and (Users.username like "%$it%") }
    email?.let { op = op and (Users.email like "%$it%") }
    active?.let { op = op and (Users.active eq it) }
    
    // Query base
    var query = User.find(op)
    
    // Aplicar filtro por rol si es necesario
    if (roleId != null) {
        val roleUsers = UserRoles
            .select { UserRoles.role eq roleId }
            .map { it[UserRoles.user] }
        
        query = query.adjustWhere { Users.id inList roleUsers }
    }
    
    query.toList()
}
```

### Búsqueda de texto completo

PostgreSQL:
```kotlin
fun fullTextSearch(query: String): List<Post> = transaction {
    exec("SELECT p.id FROM posts p WHERE to_tsvector('english', p.title || ' ' || p.content) @@ to_tsquery('english', ?)",
        listOf(TextColumnType() to query.split(" ").joinToString(" & "))
    ) { rs ->
        val postIds = mutableListOf<Int>()
        while (rs.next()) {
            postIds.add(rs.getInt(1))
        }
        postIds
    }?.let { ids ->
        if (ids.isEmpty()) return@transaction emptyList()
        Post.find { Posts.id inList ids }.toList()
    } ?: emptyList()
}
```

## Operaciones masivas

### Inserción masiva

```kotlin
fun bulkInsertUsers(users: List<UserData>): Int = transaction {
    var insertedCount = 0
    
    Users.batchInsert(users) { userData ->
        this[Users.username] = userData.username
        this[Users.email] = userData.email
        this[Users.passwordHash] = BCrypt.hashpw(userData.password, BCrypt.gensalt())
        
        insertedCount++
    }
    
    insertedCount
}
```

### Actualización masiva

```kotlin
fun deactivateInactiveUsers(lastLoginBefore: LocalDateTime): Int = transaction {
    Users.update({ Users.lastLogin less lastLoginBefore and (Users.active eq true) }) {
        it[active] = false
    }
}
```

### Eliminación masiva

```kotlin
fun purgeOldMessages(before: LocalDateTime): Int = transaction {
    Messages.deleteWhere { Messages.createdAt less before }
}
```

## Datos jerárquicos

### Estructura de árbol (autorreferencial)

```kotlin
object Categories : IntIdTable() {
    val name = varchar("name", 100)
    val parentId = reference("parent_id", Categories).nullable()
}

class Category(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Category>(Categories)
    
    var name by Categories.name
    var parent by Category optionalReferencedOn Categories.parentId
    val children by Category referrersOn Categories.parentId
}

// Obtener árbol completo
fun getCategoryTree(rootId: Int? = null): List<CategoryNode> = transaction {
    // Encontrar categorías raíz o una específica
    val roots = if (rootId != null) {
        Category.find { Categories.id eq rootId }
    } else {
        Category.find { Categories.parentId.isNull() }
    }
    
    // Construir árbol recursivamente
    roots.map { buildCategoryNode(it) }
}

fun buildCategoryNode(category: Category): CategoryNode = transaction {
    val children = category.children.map { buildCategoryNode(it) }
    CategoryNode(category.id.value, category.name, children)
}

data class CategoryNode(
    val id: Int,
    val name: String,
    val children: List<CategoryNode> = emptyList()
)
```

### Consulta de ancestros

```kotlin
fun getAncestors(categoryId: Int): List<Category> = transaction {
    val ancestors = mutableListOf<Category>()
    var current = Category.findById(categoryId)?.parent
    
    while (current != null) {
        ancestors.add(current)
        current = current.parent
    }
    
    ancestors.reverse()
    ancestors
}
```

## Almacenamiento de archivos

### Metadatos de archivos con blob

```kotlin
object Files : IntIdTable() {
    val name = varchar("name", 255)
    val contentType = varchar("content_type", 100)
    val size = long("size")
    val content = blob("content")
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)
    val uploader = reference("uploader_id", Users)
}

class File(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<File>(Files)
    
    var name by Files.name
    var contentType by Files.contentType
    var size by Files.size
    var content by Files.content
    var uploadedAt by Files.uploadedAt
    var uploader by User referencedOn Files.uploader
}

fun uploadFile(fileName: String, contentType: String, fileData: ByteArray, userId: Int): File = transaction {
    File.new {
        name = fileName
        this.contentType = contentType
        size = fileData.size.toLong()
        content = ExposedBlob(fileData)
        uploader = User[userId]
    }
}

fun downloadFile(fileId: Int): Pair<FileMetadata, ByteArray>? = transaction {
    File.findById(fileId)?.let { file ->
        val metadata = FileMetadata(
            id = file.id.value,
            name = file.name,
            contentType = file.contentType,
            size = file.size
        )
        
        metadata to file.content.bytes
    }
}

data class FileMetadata(
    val id: Int,
    val name: String,
    val contentType: String,
    val size: Long
)
```

### Referencias a archivos externos

```kotlin
object FileReferences : IntIdTable() {
    val name = varchar("name", 255)
    val path = varchar("path", 500)
    val contentType = varchar("content_type", 100)
    val size = long("size")
    val storageType = enumeration("storage_type", StorageType::class)
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)
    val uploader = reference("uploader_id", Users)
}

enum class StorageType {
    LOCAL_FILESYSTEM,
    S3,
    AZURE_BLOB
}
```

## Implementación de soft delete

### Enfoque básico

```kotlin
object Users : IntIdTable() {
    val username = varchar("username", 50)
    val email = varchar("email", 100)
    val isDeleted = bool("is_deleted").default(false)
    val deletedAt = datetime("deleted_at").nullable()
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users) {
        fun findActive() = find { Users.isDeleted eq false }
    }
    
    var username by Users.username
    var email by Users.email
    var isDeleted by Users.isDeleted
    var deletedAt by Users.deletedAt
    
    fun softDelete() = transaction {
        isDeleted = true
        deletedAt = LocalDateTime.now()
    }
}

// Uso
val users = User.findActive().toList()
user.softDelete()
```

### Enfoque avanzado con interceptor

```kotlin
abstract class SoftDeleteTable(name: String = "") : IntIdTable(name) {
    val isDeleted = bool("is_deleted").default(false)
    val deletedAt = datetime("deleted_at").nullable()
}

abstract class SoftDeleteEntityClass<E : SoftDeleteEntity>(table: SoftDeleteTable) : IntEntityClass<E>(table) {
    fun findActive() = find { (table as SoftDeleteTable).isDeleted eq false }
    
    override fun wrapRow(row: ResultRow): E {
        return super.wrapRow(row)
    }
    
    override fun all(): SizedIterable<E> = findActive()
}

abstract class SoftDeleteEntity(id: EntityID<Int>) : IntEntity(id) {
    abstract var isDeleted: Boolean
    abstract var deletedAt: LocalDateTime?
    
    fun softDelete() = transaction {
        isDeleted = true
        deletedAt = LocalDateTime.now()
    }
}
```

## Auditoría y registro de cambios

### Campos de auditoría básicos

```kotlin
interface AuditableTable {
    val createdAt: Column<LocalDateTime>
    val updatedAt: Column<LocalDateTime>
    val createdBy: Column<EntityID<Int>>
    val updatedBy: Column<EntityID<Int>>
}

abstract class AuditableIntIdTable(name: String = "") : IntIdTable(name), AuditableTable {
    override val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    override val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    override val createdBy = reference("created_by", Users)
    override val updatedBy = reference("updated_by", Users)
}

// Uso
object Invoices : AuditableIntIdTable() {
    val number = varchar("number", 20)
    val amount = decimal("amount", 10, 2)
}
```

### Registro de historial de cambios

```kotlin
object AuditLog : IntIdTable() {
    val entityType = varchar("entity_type", 50)
    val entityId = integer("entity_id")
    val action = enumeration("action", AuditAction::class)
    val changedBy = reference("changed_by", Users)
    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
    val oldValues = text("old_values").nullable()
    val newValues = text("new_values").nullable()
}

enum class AuditAction {
    CREATE, UPDATE, DELETE
}

// Extensión para logging
fun <T : Entity<Int>> T.logChange(
    action: AuditAction, 
    userId: Int,
    oldValues: Map<String, Any?>? = null,
    newValues: Map<String, Any?>? = null
) = transaction {
    val entityType = this@logChange::class.simpleName ?: "Unknown"
    
    AuditLog.insert {
        it[entityType] = entityType
        it[entityId] = this@logChange.id.value
        it[AuditLog.action] = action
        it[changedBy] = EntityID(userId, Users)
        it[oldValues] = oldValues?.let { vals -> Json.encodeToString(vals) }
        it[newValues] = newValues?.let { vals -> Json.encodeToString(vals) }
    }
}
```

## Cache y rendimiento

### Implementación de cache simple

```kotlin
class UserRepository {
    private val cache = ConcurrentHashMap<Int, UserDto>()
    
    fun getById(id: Int): UserDto? {
        // 1. Intentar obtener del cache
        cache[id]?.let { return it }
        
        // 2. Si no está en cache, buscar en DB
        return transaction {
            User.findById(id)?.let { user ->
                val dto = user.toDto()
                // 3. Guardar en cache
                cache[id] = dto
                dto
            }
        }
    }
    
    fun invalidateCache(id: Int) {
        cache.remove(id)
    }
    
    fun clearCache() {
        cache.clear()
    }
}
```

### Optimización de consultas complejas

```kotlin
fun getPopularPostsWithAuthors(limit: Int): List<PostWithAuthorDto> = transaction {
    // Consulta optimizada con join
    (Posts innerJoin Users)
        .slice(
            Posts.id, Posts.title, Posts.views,
            Users.id, Users.username
        )
        .select { Posts.views greater 100 }
        .orderBy(Posts.views to SortOrder.DESC)
        .limit(limit)
        .map {
            PostWithAuthorDto(
                id = it[Posts.id].value,
                title = it[Posts.title],
                views = it[Posts.views],
                authorId = it[Users.id].value,
                authorName = it[Users.username]
            )
        }
}
```

## Geolocalización

### Coordenadas y distancias

```kotlin
object Locations : IntIdTable() {
    val name = varchar("name", 100)
    val latitude = double("latitude")
    val longitude = double("longitude")
}

// Función para calcular distancias (Fórmula Haversine)
fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Expression<Double> {
    val earthRadius = 6371.0 // km
    
    val dLat = CustomFunction<Double>("RADIANS", DoubleColumnType(), ExpressionWithColumnType.wrap(lat2 - lat1))
    val dLon = CustomFunction<Double>("RADIANS", DoubleColumnType(), ExpressionWithColumnType.wrap(lon2 - lon1))
    
    val a = CustomFunction<Double>(
        "SIN", DoubleColumnType(), dLat / ExpressionWithColumnType.wrap(2.0)
    ).pow(2) + CustomFunction<Double>(
        "COS", DoubleColumnType(), ExpressionWithColumnType.wrap(lat1.toRadians())
    ) * CustomFunction<Double>(
        "COS", DoubleColumnType(), ExpressionWithColumnType.wrap(lat2.toRadians())
    ) * CustomFunction<Double>(
        "SIN", DoubleColumnType(), dLon / ExpressionWithColumnType.wrap(2.0)
    ).pow(2)
    
    val c = CustomFunction<Double>(
        "ASIN", DoubleColumnType(), ExpressionWithColumnType.wrap(a.sqrt().coerceIn(0.0, 1.0))
    ) * ExpressionWithColumnType.wrap(2.0)
    
    return c * ExpressionWithColumnType.wrap(earthRadius)
}

// Encontrar ubicaciones cercanas
fun findNearbyLocations(lat: Double, lon: Double, maxDistanceKm: Double): List<LocationDto> = transaction {
    val distance = distanceBetween(
        ExpressionWithColumnType.wrap(lat.toRadians()), 
        ExpressionWithColumnType.wrap(lon.toRadians()),
        CustomFunction<Double>("RADIANS", DoubleColumnType(), Locations.latitude),
        CustomFunction<Double>("RADIANS", DoubleColumnType(), Locations.longitude)
    ).alias("distance")
    
    Locations
        .slice(Locations.id, Locations.name, Locations.latitude, Locations.longitude, distance)
        .selectAll()
        .having { distance less maxDistanceKm }
        .orderBy(distance to SortOrder.ASC)
        .map {
            LocationDto(
                id = it[Locations.id].value,
                name = it[Locations.name],
                latitude = it[Locations.latitude],
                longitude = it[Locations.longitude],
                distanceKm = it[distance]
            )
        }
}
```

## Resumen

Este documento ha presentado soluciones para casos de uso comunes al trabajar con Exposed:

1. **Autenticación y autorización** - Implementación de usuarios, roles y tokens
2. **Paginación** - Estrategias para dividir grandes conjuntos de datos
3. **Búsqueda y filtrado** - Técnicas para realizar búsquedas precisas y flexibles
4. **Operaciones masivas** - Procesamiento eficiente de grandes volúmenes de datos
5. **Datos jerárquicos** - Manejo de estructuras de árbol y relaciones recursivas
6. **Almacenamiento de archivos** - Opciones para guardar y referenciar archivos
7. **Soft delete** - Técnicas para borrados lógicos en lugar de físicos
8. **Auditoría y cambios** - Registro de modificaciones en los datos
9. **Cache y rendimiento** - Optimización de consultas frecuentes
10. **Geolocalización** - Funciones para trabajar con coordenadas y distancias

Estos patrones pueden adaptarse y combinarse según los requisitos específicos de tu aplicación.