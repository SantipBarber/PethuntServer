# Operaciones CRUD con la API DSL de Exposed

La API DSL (Domain-Specific Language) de Exposed proporciona una forma expresiva y tipo segura de realizar operaciones CRUD (Create, Read, Update, Delete) en tu base de datos. Esta API está diseñada para ser similar a SQL, manteniendo la seguridad de tipos de Kotlin.

## Configuración previa

Para los ejemplos en este documento, asumiremos las siguientes definiciones de tablas:

```kotlin
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    val age = integer("age").nullable()
    val isActive = bool("is_active").default(true)
    
    override val primaryKey = PrimaryKey(id)
}

object Posts : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users)
    val title = varchar("title", 100)
    val content = text("content")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    
    override val primaryKey = PrimaryKey(id)
}
```

## Create (Crear)

### Insertar un solo registro

Para insertar un registro en una tabla, utiliza el método `insert`:

```kotlin
val userId = transaction {
    Users.insert {
        it[name] = "John Doe"
        it[email] = "john@example.com"
        it[age] = 30
    } get Users.id
}
```

En este ejemplo:
- La función `insert` recibe un bloque donde especificas los valores para cada columna.
- `it` representa la instrucción de inserción.
- `get Users.id` recupera el ID generado automáticamente.

### Insertar y obtener todos los valores generados

Si necesitas recuperar más valores generados automáticamente (como timestamps default):

```kotlin
val postRow = transaction {
    Posts.insert {
        it[userId] = 1
        it[title] = "Mi primer post"
        it[content] = "Contenido del post..."
    }
}

val createdAt = postRow[Posts.createdAt]
```

### Insertar e ignorar errores

En algunas bases de datos, puedes usar `insertIgnore` para insertar registros ignorando errores de duplicación:

```kotlin
transaction {
    Users.insertIgnore {
        it[name] = "John Doe"
        it[email] = "john@example.com" // Si este email ya existe, no se lanzará error
    }
}
```

### Insertar datos desde un SELECT

Puedes insertar datos desde los resultados de una consulta:

```kotlin
transaction {
    val activeEmails = Users
        .slice(Users.email)
        .select { Users.isActive eq true }
    
    // Copiar emails activos a otra tabla
    EmailList.insert(activeEmails)
}
```

### Inserciones por lotes

Para insertar múltiples registros eficientemente:

```kotlin
transaction {
    val users = listOf(
        Triple("Alice Smith", "alice@example.com", 28),
        Triple("Bob Johnson", "bob@example.com", 35),
        Triple("Carol Williams", "carol@example.com", 42)
    )
    
    Users.batchInsert(users) { (name, email, age) ->
        this[Users.name] = name
        this[Users.email] = email
        this[Users.age] = age
    }
}
```

## Read (Leer)

### Seleccionar todos los registros

Para seleccionar todos los registros de una tabla:

```kotlin
transaction {
    val allUsers = Users.selectAll().toList()
    
    // Puedes acceder a los campos de cada registro
    allUsers.forEach {
        println("ID: ${it[Users.id]}, Name: ${it[Users.name]}")
    }
}
```

### Seleccionar con condiciones (WHERE)

Para filtrar registros con condiciones:

```kotlin
transaction {
    val activeUsers = Users.select { Users.isActive eq true }
    val olderUsers = Users.select { Users.age greaterEq 30 }
    val specificEmailDomain = Users.select { Users.email like "%@gmail.com" }
    
    // Condiciones compuestas
    val youngActiveUsers = Users.select {
        (Users.age less 30) and (Users.isActive eq true)
    }
    
    val usersToContact = Users.select {
        (Users.email like "%@example.com") or (Users.email like "%@gmail.com")
    }
}
```

### Operadores de comparación

Exposed proporciona muchos operadores para construir condiciones:

```kotlin
// Igualdad
Users.select { Users.id eq 1 }

// Desigualdad
Users.select { Users.id neq 1 }

// Mayor que, Menor que
Users.select { Users.age greater 18 }
Users.select { Users.age less 65 }
Users.select { Users.age greaterEq 21 }
Users.select { Users.age lessEq 60 }

// Entre valores
Users.select { Users.age.between(18, 65) }

// En lista de valores
Users.select { Users.id inList listOf(1, 2, 3) }

// Es nulo / No es nulo
Users.select { Users.age.isNull() }
Users.select { Users.age.isNotNull() }

// Like (comodines)
Users.select { Users.name like "J%" } // Nombres que empiezan con J
Users.select { Users.name notLike "J%" } // Nombres que no empiezan con J

// Expresiones regulares (en bases de datos compatibles)
Users.select { Users.email regexp ".*@gmail\\.com$" }
```

### Seleccionar columnas específicas

Para seleccionar solo ciertas columnas:

```kotlin
transaction {
    val namesAndEmails = Users.slice(Users.name, Users.email).selectAll()
    
    namesAndEmails.forEach {
        println("Name: ${it[Users.name]}, Email: ${it[Users.email]}")
    }
}
```

### Ordenar resultados

Para ordenar los resultados:

```kotlin
transaction {
    val usersOrderedByName = Users
        .selectAll()
        .orderBy(Users.name to SortOrder.ASC)
    
    // Ordenar por múltiples columnas
    val sortedUsers = Users
        .selectAll()
        .orderBy(
            Users.age to SortOrder.DESC,
            Users.name to SortOrder.ASC
        )
}
```

### Limitar y paginar resultados

Para limitar o paginar resultados:

```kotlin
transaction {
    // Primeros 10 usuarios
    val first10Users = Users.selectAll().limit(10)
    
    // Paginación: página 3, 10 por página
    val page = 3
    val pageSize = 10
    val paginatedUsers = Users
        .selectAll()
        .limit(pageSize, offset = (page - 1L) * pageSize)
}
```

### Funciones de agregación

Para aplicar funciones de agregación:

```kotlin
transaction {
    // Contar usuarios
    val userCount = Users.selectAll().count()
    
    // Edad promedio
    val avgAge = Users.slice(Users.age.avg()).selectAll().single()[Users.age.avg()]
    
    // Edad máxima
    val maxAge = Users.slice(Users.age.max()).selectAll().single()[Users.age.max()]
    
    // Múltiples agregaciones
    val ageStats = Users
        .slice(Users.age.min(), Users.age.max(), Users.age.avg(), Users.age.count())
        .selectAll()
        .single()
    
    println("Min age: ${ageStats[Users.age.min()]}")
    println("Max age: ${ageStats[Users.age.max()]}")
    println("Avg age: ${ageStats[Users.age.avg()]}")
    println("Count: ${ageStats[Users.age.count()]}")
}
```

### Group By

Para agrupar resultados:

```kotlin
transaction {
    // Contar usuarios por edad
    val usersByAge = Users
        .slice(Users.age, Users.id.count())
        .selectAll()
        .groupBy(Users.age)
        .map { 
            it[Users.age] to it[Users.id.count()]
        }
    
    // Número de posts por usuario
    val postsByUser = Posts
        .slice(Posts.userId, Posts.id.count())
        .selectAll()
        .groupBy(Posts.userId)
}
```

### Encontrar un solo resultado

Cuando esperas un solo resultado:

```kotlin
transaction {
    // Obtener un usuario específico por ID
    val user = Users.select { Users.id eq 1 }.single()
    
    // O con null si no existe
    val userOrNull = Users.select { Users.email eq "nonexistent@example.com" }.singleOrNull()
}
```

## Update (Actualizar)

### Actualizar registros

Para actualizar registros:

```kotlin
transaction {
    // Actualizar un solo campo para todos los usuarios activos
    val updatedRows = Users.update({ Users.isActive eq true }) {
        it[name] = "Active User"
    }
    
    // Actualizar múltiples campos para un usuario específico
    Users.update({ Users.id eq 1 }) {
        it[name] = "Updated Name"
        it[email] = "updated@example.com"
        it[age] = 31
    }
}
```

### Actualizar con expresiones

Puedes usar expresiones en las actualizaciones:

```kotlin
transaction {
    // Incrementar la edad de todos los usuarios en 1
    Users.update {
        with(SqlExpressionBuilder) {
            it[age] = age + 1
        }
    }
    
    // Actualizar campos basados en sus valores actuales
    Users.update({ Users.id eq 1 }) {
        with(SqlExpressionBuilder) {
            it[name] = name.upperCase()
            it[age] = age?.plus(5) // Nota el uso de '?' para campos nullables
        }
    }
}
```

## Delete (Eliminar)

### Eliminar registros

Para eliminar registros:

```kotlin
transaction {
    // Eliminar un usuario específico
    val deletedRows = Users.deleteWhere { Users.id eq 1 }
    
    // Eliminar usuarios inactivos
    Users.deleteWhere { Users.isActive eq false }
    
    // Eliminar usuarios con condiciones compuestas
    Users.deleteWhere {
        (Users.age less 18) or (Users.email like "%@temporary.com")
    }
    
    // Eliminar todos los registros de una tabla
    Posts.deleteAll()
}
```

## Upsert (Insert or Update)

La operación upsert inserta un nuevo registro o actualiza uno existente si ya existe (basado en restricciones de unicidad):

```kotlin
transaction {
    Users.upsert {
        it[email] = "john@example.com" // Si esta columna tiene un índice único
        it[name] = "John Doe"
        it[age] = 31
    }
}
```

En bases de datos como PostgreSQL, puedes especificar las columnas de conflicto y el comportamiento de actualización:

```kotlin
transaction {
    Users.upsert(
        Users.email, // Columna de conflicto
        onUpdate = { // Qué hacer en caso de conflicto
            it[name] = "Updated Name"
            it[age] = Users.age + 1 // Incrementar edad
        }
    ) {
        it[email] = "john@example.com"
        it[name] = "John Doe"
        it[age] = 30
    }
}
```

## Replace (Reemplazar)

En algunas bases de datos (SQLite, MySQL), puedes usar `replace` para eliminar filas existentes y insertar nuevas:

```kotlin
transaction {
    Users.replace {
        it[id] = 1 // Si existe un usuario con id=1, se eliminará primero
        it[name] = "Replacement User"
        it[email] = "replaced@example.com"
        it[age] = 40
    }
}
```

## Transacciones

Todas las operaciones en Exposed deben estar dentro de un bloque `transaction`:

```kotlin
transaction {
    // Las operaciones aquí se ejecutan como una sola transacción
    val userId = Users.insert {
        it[name] = "New User"
        it[email] = "new@example.com"
    } get Users.id
    
    Posts.insert {
        it[this.userId] = userId
        it[title] = "First Post"
        it[content] = "Hello, world!"
    }
    
    // Si ocurre una excepción, se hace rollback de todo
}
```

### Transacciones anidadas

Puedes anidar transacciones:

```kotlin
transaction {
    val user = Users.insert {
        it[name] = "Outer Transaction User"
        it[email] = "outer@example.com"
    }
    
    try {
        transaction {
            // Esta transacción anidada por defecto comparte recursos con la externa
            Posts.insert {
                it[userId] = user[Users.id]
                it[title] = "Inner Transaction Post"
                it[content] = "This might fail..."
            }
            
            // Si lanzamos una excepción aquí, también se hará rollback de la transacción externa
            if (someCondition) throw Exception("Something went wrong")
        }
    } catch (e: Exception) {
        // Manejar la excepción
    }
}
```

Para que las transacciones anidadas sean independientes, configura la base de datos:

```kotlin
val db = Database.connect(/* configuración */)
db.useNestedTransactions = true

// Ahora las transacciones anidadas utilizan savepoints y pueden hacer rollback independientemente
```

### Transacciones con corrutinas

Para usar Exposed con corrutinas:

```kotlin
suspend fun getUsers(): List<Map<String, Any>> = newSuspendedTransaction(Dispatchers.IO) {
    Users.selectAll().map {
        mapOf(
            "id" to it[Users.id],
            "name" to it[Users.name],
            "email" to it[Users.email]
        )
    }
}

// Uso con corrutinas
launch {
    val users = getUsers()
    // Hacer algo con los usuarios
}
```

## Rendimiento y Optimizaciones

### Eager Loading

Por defecto, Exposed carga los datos de tipo BLOB y TEXT de forma lazy. Para cargar estos campos de forma eager:

```kotlin
object Articles : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 200)
    val content = text("content", eagerLoading = true) // Carga inmediata
    
    override val primaryKey = PrimaryKey(id)
}
```

### Batch Operations

Para operaciones por lotes más eficientes:

```kotlin
transaction {
    val dataToInsert = (1..1000).map { i ->
        Triple("User $i", "user$i@example.com", i % 100)
    }
    
    // Inserta en lotes
    Users.batchInsert(dataToInsert) { (name, email, age) ->
        this[Users.name] = name
        this[Users.email] = email
        this[Users.age] = age
    }
}
```

## Resumen

La API DSL de Exposed ofrece una forma potente y tipo segura de realizar operaciones CRUD:

- **Create**: `insert()`, `batchInsert()`, `insertIgnore()`, `insertAndGetId()`
- **Read**: `select()`, `selectAll()`, slice(), ordenación, agrupación y paginación
- **Update**: `update()` con condiciones y expresiones
- **Delete**: `deleteWhere()`, `deleteAll()`
- **Otras**: `upsert()`, `replace()`

Todas estas operaciones deben ejecutarse dentro de un bloque `transaction` para asegurar la consistencia de los datos.