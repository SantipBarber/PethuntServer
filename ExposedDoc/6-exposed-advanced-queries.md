# Queries avanzadas en Exposed

Este documento explora técnicas avanzadas para construir y ejecutar consultas complejas con Exposed. Cubriremos desde operaciones de join hasta subqueries, funciones SQL personalizadas y optimizaciones de rendimiento.

## Configuración previa

Para los ejemplos, asumiremos las siguientes definiciones de tablas:

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    val age = integer("age").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(id)
}

object Posts : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users)
    val title = varchar("title", 100)
    val content = text("content")
    val views = integer("views").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(id)
}

object Comments : Table() {
    val id = integer("id").autoIncrement()
    val postId = reference("post_id", Posts)
    val userId = reference("user_id", Users)
    val content = text("content")
    val createdAt = datetime("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(id)
}

object Tags : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    
    override val primaryKey = PrimaryKey(id)
}

object PostTags : Table() {
    val postId = reference("post_id", Posts)
    val tagId = reference("tag_id", Tags)
    
    override val primaryKey = PrimaryKey(postId, tagId)
}
```

## Joins avanzados

### Inner Join

El inner join selecciona registros que tienen coincidencias en ambas tablas:

```kotlin
transaction {
    // Obtener posts con sus autores
    val postsWithAuthors = (Posts innerJoin Users)
        .select { Users.isActive eq true }
        .orderBy(Posts.createdAt to SortOrder.DESC)
        .map { row ->
            mapOf(
                "postId" to row[Posts.id],
                "title" to row[Posts.title],
                "author" to row[Users.name]
            )
        }
}
```

### Left Join

El left join selecciona todos los registros de la primera tabla, y los registros coincidentes de la segunda:

```kotlin
transaction {
    // Obtener todos los usuarios y sus posts (si tienen)
    val usersWithPosts = (Users leftJoin Posts)
        .slice(Users.name, Users.email, Posts.title.nullable())
        .selectAll()
        .map { row ->
            mapOf(
                "userName" to row[Users.name],
                "userEmail" to row[Users.email],
                "postTitle" to row[Posts.title.nullable()]
            )
        }
}
```

### Full Join

El full join selecciona todos los registros cuando hay una coincidencia en cualquiera de las tablas:

```kotlin
transaction {
    // Solo disponible en bases de datos que lo soportan (PostgreSQL, Oracle, SQL Server)
    val fullJoinResult = (Users fullJoin Posts)
        .slice(Users.name.nullable(), Posts.title.nullable())
        .selectAll()
        .map { row ->
            mapOf(
                "userName" to row[Users.name.nullable()],
                "postTitle" to row[Posts.title.nullable()]
            )
        }
}
```

### Cross Join

El cross join crea un producto cartesiano entre las dos tablas:

```kotlin
transaction {
    val crossJoinResult = (Users crossJoin Tags)
        .slice(Users.name, Tags.name)
        .selectAll()
        .map { row ->
            "${row[Users.name]} - ${row[Tags.name]}"
        }
}
```

### Self Join

El self join une una tabla consigo misma:

```kotlin
transaction {
    // Ejemplo: Encontrar pares de usuarios con la misma edad
    val sameAgeUsers = Users.alias("u1")
    val otherUsers = Users.alias("u2")
    
    (sameAgeUsers innerJoin otherUsers)
        .select {
            (sameAgeUsers[Users.age] eq otherUsers[Users.age]) and
            (sameAgeUsers[Users.id] less otherUsers[Users.id]) // Evitar duplicados
        }
        .map { row ->
            "Usuario ${row[sameAgeUsers[Users.name]]} y ${row[otherUsers[Users.name]]} tienen la misma edad: ${row[sameAgeUsers[Users.age]]}"
        }
}
```

### Joins múltiples

Puedes encadenar múltiples joins:

```kotlin
transaction {
    val result = (Posts innerJoin Users innerJoin Comments leftJoin Tags.alias("t") innerJoin PostTags)
        .select {
            (PostTags.tagId eq Users.alias("t")[Tags.id]) and
            (Users.isActive eq true)
        }
        .groupBy(Posts.id)
}
```

## Subconsultas

Las subconsultas son consultas anidadas dentro de otra consulta.

### Subconsulta en la cláusula FROM

```kotlin
transaction {
    // Subconsulta que cuenta los posts por usuario
    val postCountSubquery = Posts
        .slice(Posts.userId, Posts.id.count())
        .selectAll()
        .groupBy(Posts.userId)
        .alias("post_counts")
    
    // Columnas de la subconsulta
    val userIdAlias = Posts.userId.alias("user_id")
    val postCountAlias = Posts.id.count().alias("post_count")
    
    // Consulta principal que usa la subconsulta
    val usersWithPostCount = Users
        .join(postCountSubquery, JoinType.INNER, onColumn = Users.id, otherColumn = userIdAlias)
        .slice(Users.name, postCountAlias)
        .selectAll()
        .map { row ->
            "${row[Users.name]} tiene ${row[postCountAlias]} posts"
        }
}
```

### Subconsulta en la cláusula WHERE

```kotlin
transaction {
    // Encontrar usuarios que han escrito posts con más de 100 vistas
    val usersWithPopularPosts = Users.select {
        Users.id inSubQuery Posts
            .slice(Posts.userId)
            .select { Posts.views greater 100 }
    }
    
    // Encontrar usuarios sin posts
    val usersWithoutPosts = Users.select {
        Users.id notInSubQuery Posts
            .slice(Posts.userId)
            .selectAll()
    }
}
```

### Subconsulta en la cláusula SELECT

```kotlin
transaction {
    // Para cada usuario, mostrar también su último post
    val lastPostQuery = Posts
        .slice(Posts.title)
        .select { Posts.userId eq Users.id }
        .orderBy(Posts.createdAt to SortOrder.DESC)
        .limit(1)
        .alias("last_post")
    
    val usersWithLastPost = Users
        .slice(Users.name, lastPostQuery)
        .selectAll()
        .map { row ->
            "${row[Users.name]} - Último post: ${row.getOrNull(lastPostQuery)}"
        }
}
```

## Expresiones y funciones SQL

### Expresiones personalizadas

```kotlin
transaction {
    // Concatenar nombre y email
    val fullInfo = concat(Users.name, stringLiteral(" <"), Users.email, stringLiteral(">"))
        .alias("full_info")
    
    Users.slice(fullInfo)
        .selectAll()
        .forEach {
            println(it[fullInfo])
        }
    
    // Expresiones matemáticas
    val discountedViews = (Posts.views.castTo<Double>(DoubleColumnType()) * 0.9)
        .alias("discounted_views")
    
    Posts.slice(Posts.title, discountedViews)
        .selectAll()
        .forEach {
            println("${it[Posts.title]}: ${it[discountedViews]}")
        }
}
```

### Funciones de texto

```kotlin
transaction {
    // Conversión a mayúsculas
    val upperName = Users.name.upperCase()
    
    // Longitud del texto
    val nameLength = Users.name.charLength()
    
    // Substring
    val nameInitials = Users.name.substring(1, 1)
    
    Users.slice(Users.name, upperName, nameLength, nameInitials)
        .selectAll()
        .forEach {
            println("${it[Users.name]} -> ${it[upperName]} (${it[nameLength]} chars, inicia con ${it[nameInitials]})")
        }
}
```

### Funciones de fecha y hora

```kotlin
transaction {
    // Extraer año de una fecha
    val year = extract(Posts.createdAt, DatePart.YEAR).alias("year")
    val month = extract(Posts.createdAt, DatePart.MONTH).alias("month")
    
    // Filtrar por rango de fechas
    val recentPosts = Posts.select {
        Posts.createdAt between (LocalDateTime.now().minusDays(30)) and LocalDateTime.now()
    }
    
    // Agrupar por año/mes
    val postsByYearMonth = Posts
        .slice(year, month, Posts.id.count())
        .selectAll()
        .groupBy(year, month)
        .orderBy(year to SortOrder.DESC, month to SortOrder.DESC)
        .map {
            "${it[year]}-${it[month]}: ${it[Posts.id.count()]} posts"
        }
}
```

### Funciones de agregación

```kotlin
transaction {
    // Total, promedio, mínimo y máximo de vistas
    val viewStats = Posts
        .slice(
            Posts.views.sum().alias("total_views"),
            Posts.views.avg().alias("avg_views"),
            Posts.views.min().alias("min_views"),
            Posts.views.max().alias("max_views"),
            Posts.views.count().alias("post_count")
        )
        .selectAll()
        .single()
    
    println("Estadísticas de vistas:")
    println("Total: ${viewStats[Posts.views.sum()]}")
    println("Promedio: ${viewStats[Posts.views.avg()]}")
    println("Mínimo: ${viewStats[Posts.views.min()]}")
    println("Máximo: ${viewStats[Posts.views.max()]}")
    println("Cantidad de posts: ${viewStats[Posts.views.count()]}")
}
```

### Funciones de agrupación avanzadas

```kotlin
transaction {
    // HAVING para filtrar después de agrupar
    val popularTagsByPostCount = (Tags innerJoin PostTags)
        .slice(Tags.name, PostTags.postId.count().alias("post_count"))
        .selectAll()
        .groupBy(Tags.name)
        .having { PostTags.postId.count() greater 5 }
        .orderBy(PostTags.postId.count() to SortOrder.DESC)
        .map {
            "${it[Tags.name]}: ${it[PostTags.postId.count()]} posts"
        }
}
```

### Funciones de ventana

Las funciones de ventana (Window Functions) permiten realizar cálculos a través de un conjunto de filas relacionadas con la fila actual:

```kotlin
transaction {
    // Ranking de posts por vistas
    val rankByViews = rank()
        .over()
        .orderBy(Posts.views to SortOrder.DESC)
        .alias("rank")
    
    // Promedio acumulativo
    val runningAvgViews = Posts.views.avg()
        .over()
        .orderBy(Posts.createdAt)
        .alias("running_avg")
    
    // Suma acumulativa
    val runningSumViews = Posts.views.sum()
        .over()
        .orderBy(Posts.createdAt)
        .alias("running_sum")
    
    // Número de fila
    val rowNumber = rowNumber()
        .over()
        .orderBy(Posts.createdAt)
        .alias("row_num")
    
    // Particiones: ranking dentro de cada usuario
    val rankByViewsPerUser = rank()
        .over()
        .partitionBy(Posts.userId)
        .orderBy(Posts.views to SortOrder.DESC)
        .alias("rank_per_user")
    
    Posts.slice(
        Posts.id, Posts.title, Posts.views, 
        rankByViews, runningAvgViews, runningSumViews, 
        rowNumber, rankByViewsPerUser
    )
    .selectAll()
    .map {
        mapOf(
            "id" to it[Posts.id],
            "title" to it[Posts.title],
            "views" to it[Posts.views],
            "rank" to it[rankByViews],
            "avgRunning" to it[runningAvgViews],
            "sumRunning" to it[runningSumViews],
            "rowNum" to it[rowNumber],
            "rankPerUser" to it[rankByViewsPerUser]
        )
    }
}
```

## Operaciones de conjuntos (UNION, INTERSECT, EXCEPT)

### UNION

Combina los resultados de dos consultas, eliminando duplicados:

```kotlin
transaction {
    // Posts de usuarios activos
    val activePosts = Posts
        .slice(Posts.title, Posts.createdAt)
        .select { Posts.userId inSubQuery Users.slice(Users.id).select { Users.isActive eq true } }
    
    // Posts con más de 100 vistas
    val popularPosts = Posts
        .slice(Posts.title, Posts.createdAt)
        .select { Posts.views greater 100 }
    
    // Unión: posts de usuarios activos o posts populares
    val unionPosts = activePosts.union(popularPosts)
        .map { "${it[Posts.title]} (${it[Posts.createdAt]})" }
}
```

### UNION ALL

Similar a UNION, pero mantiene los duplicados:

```kotlin
transaction {
    val allPosts = activePosts.unionAll(popularPosts)
        .map { "${it[Posts.title]} (${it[Posts.createdAt]})" }
}
```

### EXCEPT (MINUS)

Devuelve filas de la primera consulta que no existen en la segunda:

```kotlin
transaction {
    // Posts populares de usuarios inactivos
    val inactivePopularPosts = popularPosts.except(activePosts, true)
        .map { "${it[Posts.title]} (${it[Posts.createdAt]})" }
}
```

## Consultas dinámicas

A veces necesitas construir consultas de forma dinámica en función de criterios de filtrado opcionales:

```kotlin
transaction {
    fun findUsers(
        nameFilter: String? = null,
        minAge: Int? = null,
        isActive: Boolean? = null,
        orderBy: Pair<Column<*>, SortOrder>? = null
    ): List<ResultRow> {
        var query = Users.selectAll()
        
        // Aplicar filtros solo si están presentes
        nameFilter?.let {
            query = query.andWhere { Users.name like "%$it%" }
        }
        
        minAge?.let {
            query = query.andWhere { Users.age greaterEq it }
        }
        
        isActive?.let {
            query = query.andWhere { Users.isActive eq it }
        }
        
        // Aplicar orden si está presente
        orderBy?.let {
            query = query.orderBy(it)
        }
        
        return query.toList()
    }
    
    // Uso de la función con diferentes combinaciones de parámetros
    val activeUsers = findUsers(isActive = true)
    val olderUsers = findUsers(minAge = 30, orderBy = Users.age to SortOrder.DESC)
    val filteredUsers = findUsers(
        nameFilter = "A",
        minAge = 25,
        isActive = true,
        orderBy = Users.name to SortOrder.ASC
    )
}
```

## Optimización de consultas

### LIMIT y OFFSET para paginación

```kotlin
transaction {
    // Paginación
    val pageSize = 10
    val page = 2
    
    val paginatedPosts = Posts
        .selectAll()
        .orderBy(Posts.createdAt to SortOrder.DESC)
        .limit(pageSize, offset = (page - 1L) * pageSize)
        .toList()
}
```

### Consultas COUNT optimizadas

Para contar registros eficientemente:

```kotlin
transaction {
    // Contar todos los usuarios
    val userCount = Users.selectAll().count()
    
    // Contar posts por usuario sin cargar todos los datos
    val postCountByUser = Posts
        .slice(Posts.userId, Posts.id.count())
        .selectAll()
        .groupBy(Posts.userId)
        .associate { it[Posts.userId].value to it[Posts.id.count()] }
}
```

### Seleccionar solo las columnas necesarias

```kotlin
transaction {
    // Cargar solo lo que necesitas
    val userEmails = Users
        .slice(Users.email)
        .selectAll()
        .map { it[Users.email] }
    
    // Si solo necesitas contar, no selecciones todas las columnas
    val tagCount = Tags
        .slice(Tags.id.count())
        .selectAll()
        .single()[Tags.id.count()]
}
```

### Índices y restricciones

Asegúrate de crear índices para mejorar el rendimiento de consultas comunes:

```kotlin
object Posts : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users)
    val title = varchar("title", 100)
    val content = text("content")
    val views = integer("views").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(id)
    
    // Añadir índices para mejoras de rendimiento
    init {
        index(false, userId) // Índice para búsquedas por userId
        index(false, createdAt) // Índice para ordenar por fecha
        index(true, title) // Índice único para título (si aplica)
    }
}
```

### Batch Operations

Para operaciones en lote:

```kotlin
transaction {
    // Inserción en lote
    val posts = (1..1000).map {
        Triple("Post $it", "Contenido $it", it % 10 + 1)
    }
    
    Posts.batchInsert(posts) { (title, content, userId) ->
        this[Posts.title] = title
        this[Posts.content] = content
        this[Posts.userId] = userId
    }
    
    // Actualizar en lote (aunque sea una por una, está en una sola transacción)
    Posts.select { Posts.views less 10 }.forEach {
        Posts.update({ Posts.id eq it[Posts.id] }) { 
            it[views] = 10 
        }
    }
}
```

## Debugging y optimización

### Logging de SQL

```kotlin
transaction {
    addLogger(StdOutSqlLogger) // Imprimir SQL en consola
    
    Users.selectAll().toList() // Verás el SQL generado en la consola
}
```

### Explicar planes de consulta

En bases de datos que soportan EXPLAIN:

```kotlin
transaction {
    // Obtener el plan de ejecución (PostgreSQL, MySQL, SQLite, etc.)
    val plan = exec("EXPLAIN ANALYZE SELECT * FROM users WHERE age > 30") { rs ->
        buildString {
            while (rs.next()) {
                appendLine(rs.getString(1))
            }
        }
    }
    
    println("Plan de ejecución:")
    println(plan)
}
```

## Funciones SQL personalizadas

### Definir función SQL personalizada

```kotlin
// Definir una función SQL personalizada para extraer el dominio del email
class EmailDomain(val email: Expression<String>) : Function<String>(TextColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append("SUBSTRING_INDEX(")
            append(email)
            append(", '@', -1)")
        }
    }
}

fun Expression<String>.emailDomain() = EmailDomain(this)

// Uso
transaction {
    val emailDomainAlias = Users.email.emailDomain().alias("domain")
    
    val emailDomainCounts = Users
        .slice(emailDomainAlias, Users.id.count())
        .selectAll()
        .groupBy(emailDomainAlias)
        .orderBy(Users.id.count() to SortOrder.DESC)
        .map {
            "${it[emailDomainAlias]}: ${it[Users.id.count()]} usuarios"
        }
}
```

### Usar funciones específicas de base de datos

```kotlin
// Ejemplo para PostgreSQL: Búsqueda de texto completo
class TsQuery(val column: Expression<String>, val query: String) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append("to_tsvector(")
            append(column)
            append(") @@ to_tsquery('")
            append(query)
            append("')")
        }
    }
}

fun Expression<String>.matches(query: String) = TsQuery(this, query)

// Uso
transaction {
    val matchingPosts = Posts
        .select { Posts.content.matches("kotlin & programming") }
        .map { it[Posts.title] }
}
```

## Trabajando con JSON

Para bases de datos que soportan JSON (como PostgreSQL):

```kotlin
// Suponiendo que tengas la dependencia exposed-json
import org.jetbrains.exposed.sql.json.*

object UserSettings : Table() {
    val userId = reference("user_id", Users)
    val settings = json<UserPreferences>("settings", Json)
}

@Serializable
data class UserPreferences(
    val theme: String = "light",
    val notifications: Boolean = true,
    val language: String = "en"
)

// Consultas con JSON
transaction {
    // Filtrar por un valor dentro del JSON
    val darkThemeUsers = UserSettings
        .select { UserSettings.settings.extract<String>("theme") eq "dark" }
        .count()
    
    // Actualizar un valor dentro del JSON
    UserSettings.update({ UserSettings.userId eq 1 }) {
        with(SqlExpressionBuilder) {
            it[settings] = jsonb(
                """{"theme": "dark", "notifications": true, "language": "es"}""",
                Json
            )
        }
    }
}
```

## Trabajando con valores nulos

```kotlin
transaction {
    // Filtrar por campos nulos
    val usersWithoutAge = Users.select { Users.age.isNull() }
    
    // Filtrar por campos no nulos
    val usersWithAge = Users.select { Users.age.isNotNull() }
    
    // COALESCE para usar un valor por defecto
    val displayAge = Users.age.coalesce(0).alias("display_age")
    Users.slice(Users.name, displayAge)
        .selectAll()
        .forEach {
            println("${it[Users.name]}: ${it[displayAge]} años")
        }
    
    // CASE WHEN para lógica condicional
    val ageCategory = case()
        .When(Users.age less 18, stringLiteral("Menor"))
        .When(Users.age less 65, stringLiteral("Adulto"))
        .Else(stringLiteral("Senior"))
        .alias("age_category")
    
    Users.slice(Users.name, ageCategory)
        .selectAll()
        .forEach {
            println("${it[Users.name]} es ${it[ageCategory]}")
        }
}
```

## Transacciones anidadas

```kotlin
transaction {
    // Transacción principal
    val user = Users.insert {
        it[name] = "Nuevo Usuario"
        it[email] = "nuevo@example.com"
        it[age] = 30
    } get Users.id
    
    try {
        transaction {
            // Transacción anidada
            Posts.insert {
                it[userId] = user
                it[title] = "Mi primer post"
                it[content] = "Contenido"
            }
            
            // Si algo falla aquí, se hace rollback solo de la transacción anidada
            if (someCondition) {
                throw Exception("Error")
            }
        }
    } catch (e: Exception) {
        // Manejar error
        println("Error al crear post: ${e.message}")
    }
    
    // Esta parte aún se ejecutará aunque falle la transacción anidada
    println("Usuario creado con ID: $user")
}
```

## Resumen

Las consultas avanzadas en Exposed permiten:

1. **Joins complejos** entre múltiples tablas
2. **Subconsultas** para consultas anidadas
3. **Expresiones y funciones SQL** para operaciones avanzadas
4. **Operaciones de conjuntos** como UNION, EXCEPT
5. **Consultas dinámicas** basadas en condiciones variables
6. **Optimización de consultas** para mejorar el rendimiento
7. **Funciones SQL personalizadas** para necesidades específicas
8. **Trabajar con tipos de datos avanzados** como JSON

Dominar estas técnicas te permitirá construir aplicaciones Kotlin con Exposed que aprovechen al máximo la potencia de las bases de datos relacionales, manteniendo la seguridad de tipos y la expresividad del lenguaje Kotlin.