# Definición de tablas y modelos en Exposed

La definición de tablas es el punto de partida para trabajar con Exposed. En este documento, exploraremos cómo definir tablas, columnas, restricciones y esquemas, para modelar correctamente tu base de datos.

## Tabla básica

En Exposed, una tabla de base de datos está representada por un objeto que hereda de la clase `Table`. Para definir una tabla, simplemente crea un objeto que extienda `Table`:

```kotlin
import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    val isActive = bool("is_active").default(true)
    
    override val primaryKey = PrimaryKey(id)
}
```

Cada propiedad representa una columna en la tabla y se declara llamando a funciones como `integer()`, `varchar()`, etc., que reciben el nombre de la columna en la base de datos.

## Tipos de tablas

Exposed ofrece diferentes tipos de tablas para diversas necesidades:

### Table

La clase base para todas las definiciones de tablas. Requiere especificar manualmente el ID y la clave primaria si es necesario.

```kotlin
object Products : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val price = decimal("price", 10, 2)
    
    override val primaryKey = PrimaryKey(id)
}
```

### IdTable y sus subclases

Para simplificar la definición de tablas con una columna ID estándar, Exposed proporciona la clase base `IdTable` y sus subclases. Estas tablas generan automáticamente una columna ID apropiada.

#### IntIdTable

Genera automáticamente una columna ID de tipo entero:

```kotlin
import org.jetbrains.exposed.dao.id.IntIdTable

object Customers : IntIdTable() {
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    // La columna id se genera automáticamente
}
```

#### LongIdTable

Usa un ID de tipo Long:

```kotlin
import org.jetbrains.exposed.dao.id.LongIdTable

object Orders : LongIdTable() {
    val customerId = reference("customer_id", Customers)
    val orderDate = datetime("order_date")
}
```

#### UUIDTable

Usa un UUID como ID:

```kotlin
import org.jetbrains.exposed.dao.id.UUIDTable

object Sessions : UUIDTable() {
    val userId = reference("user_id", Users)
    val expiresAt = datetime("expires_at")
    // El id es un UUID generado automáticamente
}
```

### CompositeIdTable

Para tablas que requieren múltiples columnas como parte de la clave primaria:

```kotlin
import org.jetbrains.exposed.dao.id.CompositeIdTable

object OrderItems : CompositeIdTable() {
    val orderId = reference("order_id", Orders).entityId()
    val productId = reference("product_id", Products).entityId()
    val quantity = integer("quantity")
    
    override val primaryKey = PrimaryKey(orderId, productId)
}
```

## Personalizando el nombre de la tabla

Por defecto, Exposed genera el nombre de la tabla a partir del nombre completo de la clase. Si el nombre del objeto contiene el sufijo "Table", Exposed omitirá ese sufijo en el nombre generado.

Para especificar un nombre personalizado para la tabla, pasa el nombre al parámetro `name` del constructor de `Table`:

```kotlin
object UserProfiles : Table("user_profiles") {
    val userId = reference("user_id", Users)
    val biography = text("biography")
    val avatarUrl = varchar("avatar_url", 255).nullable()
}
```

## Tipos de datos de columnas

Exposed soporta una amplia variedad de tipos de datos de columnas:

```kotlin
object SampleTable : Table() {
    // Tipos numéricos
    val int = integer("int_column")
    val short = short("short_column")
    val long = long("long_column")
    val float = float("float_column")
    val double = double("double_column")
    val decimal = decimal("decimal_column", 10, 2) // precision, scale
    
    // Tipos de texto
    val char = char("char_column")
    val varchar = varchar("varchar_column", 100) // longitud máxima
    val text = text("text_column")
    
    // Tipos booleanos
    val bool = bool("bool_column")
    
    // Tipos binarios
    val blob = blob("blob_column")
    val binary = binary("binary_column", 1024) // longitud máxima
    
    // UUID
    val uuid = uuid("uuid_column")
    
    // Enumeración
    val status = enumeration("status", Status::class)
    
    // Referencia (clave foránea)
    val userId = reference("user_id", Users)
}

// Ejemplo de enumeración
enum class Status {
    ACTIVE, INACTIVE, PENDING
}
```

### Tipos de fecha y hora

Para trabajar con fecha y hora, Exposed proporciona extensiones específicas dependiendo de la biblioteca que uses:

#### java.time (Java 8+ Time API)

Requiere la dependencia `exposed-java-time`:

```kotlin
import org.jetbrains.exposed.sql.javatime.*

object EventsTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val date = date("date") // LocalDate
    val time = time("time") // LocalTime
    val dateTime = datetime("date_time") // LocalDateTime
    val timestamp = timestamp("timestamp") // Instant
    
    override val primaryKey = PrimaryKey(id)
}
```

#### kotlinx-datetime

Requiere la dependencia `exposed-kotlin-datetime`:

```kotlin
import org.jetbrains.exposed.sql.kotlin.datetime.*

object AppointmentsTable : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 100)
    val date = date("date") // LocalDate de kotlinx-datetime
    val time = time("time") // LocalTime de kotlinx-datetime
    val dateTime = datetime("date_time") // LocalDateTime de kotlinx-datetime
    
    override val primaryKey = PrimaryKey(id)
}
```

## Restricciones de columnas

Exposed permite añadir diferentes tipos de restricciones a las columnas:

### Valor por defecto

```kotlin
val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
val status = varchar("status", 20).default("ACTIVE")
val isDeleted = bool("is_deleted").default(false)
```

### Nullable

Por defecto, todas las columnas son `NOT NULL`. Para permitir valores nulos:

```kotlin
val middleName = varchar("middle_name", 50).nullable()
val deletedAt = datetime("deleted_at").nullable()
```

### Único

```kotlin
val email = varchar("email", 100).uniqueIndex()
```

### Check

```kotlin
val age = integer("age").check { it greaterEq 18 }
val score = integer("score").check { (it greaterEq 0) and (it lessEq 100) }
```

## Índices

### Índice simple

```kotlin
val name = varchar("name", 50).index()
```

### Índice único

```kotlin
val email = varchar("email", 100).uniqueIndex()
```

### Índice compuesto

```kotlin
object Users : Table() {
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    
    init {
        index(false, firstName, lastName) // índice no único en firstName y lastName
        uniqueIndex(firstName, lastName) // índice único en firstName y lastName
    }
}
```

## Claves primarias

La clave primaria se define sobreescribiendo la propiedad `primaryKey`:

```kotlin
object Orders : Table() {
    val id = integer("id").autoIncrement()
    val reference = varchar("reference", 10)
    
    override val primaryKey = PrimaryKey(id)
}
```

### Clave primaria compuesta

```kotlin
object OrderItems : Table() {
    val orderId = reference("order_id", Orders)
    val productId = reference("product_id", Products)
    val quantity = integer("quantity")
    
    override val primaryKey = PrimaryKey(orderId, productId)
}
```

## Claves foráneas

Las claves foráneas se definen utilizando el método `reference()`:

```kotlin
object Comments : Table() {
    val id = integer("id").autoIncrement()
    val postId = reference("post_id", Posts) // crea una FK hacia la tabla Posts
    val userId = reference("user_id", Users) // crea una FK hacia la tabla Users
    val content = text("content")
    
    override val primaryKey = PrimaryKey(id)
}
```

### Opciones de referencia

Puedes especificar el comportamiento para las acciones `ON DELETE` y `ON UPDATE`:

```kotlin
val postId = reference("post_id", Posts, onDelete = ReferenceOption.CASCADE)
```

Las opciones disponibles son:
- `ReferenceOption.RESTRICT` - Impide la eliminación/actualización de la fila referenciada
- `ReferenceOption.NO_ACTION` - Similar a RESTRICT (predeterminado para la mayoría de dialectos)
- `ReferenceOption.CASCADE` - Si la fila referenciada se elimina/actualiza, elimina/actualiza la fila que hace referencia
- `ReferenceOption.SET_NULL` - Si la fila referenciada se elimina/actualiza, establece el valor de la FK a NULL
- `ReferenceOption.SET_DEFAULT` - Si la fila referenciada se elimina/actualiza, establece el valor de la FK al valor predeterminado

### Referencias opcionales

Para relaciones opcionales, usa `optReference()`:

```kotlin
val managerId = optReference("manager_id", Employees) // permite valores NULL
```

## Esquemas

Para trabajar con esquemas de base de datos:

```kotlin
// Definir un esquema
val appSchema = Schema("app_schema")

// Crear el esquema
transaction {
    SchemaUtils.createSchema(appSchema)
}

// Definir tablas dentro del esquema
object SchemaUsers : Table("app_schema.users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    
    override val primaryKey = PrimaryKey(id)
}

// O establecer un esquema predeterminado para las operaciones
transaction {
    SchemaUtils.setSchema(appSchema)
    // Las operaciones siguientes usarán este esquema
}
```

## Creación de tablas

Para crear tablas en la base de datos, utiliza `SchemaUtils` dentro de una transacción:

```kotlin
transaction {
    // Crear una tabla
    SchemaUtils.create(Users)
    
    // Crear múltiples tablas
    SchemaUtils.create(Users, Posts, Comments)
    
    // Crear tablas y verificar si ya existen
    SchemaUtils.createMissingTablesAndColumns(Users, Posts, Comments)
}
```

## Tipos de datos JSON

Para trabajar con datos JSON, necesitas la dependencia `exposed-json`:

```kotlin
import org.jetbrains.exposed.sql.json.*

object UserSettings : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users)
    val preferences = json<UserPreferences>("preferences", Json)
    
    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class UserPreferences(
    val theme: String = "light",
    val notifications: Boolean = true,
    val language: String = "en"
)
```

## Arrays

Para bases de datos que soportan arrays (PostgreSQL, H2):

```kotlin
object Products : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val tags = array<String>("tags")
    val prices = array<Double>("prices") // Array de precios históricos
    
    override val primaryKey = PrimaryKey(id)
}
```

## Tipos personalizados

Puedes crear tipos de columna personalizados extendiendo las clases de tipo base:

```kotlin
class MyCustomTypeColumnType : ColumnType() {
    override fun sqlType(): String = "CUSTOM_TYPE"
    
    override fun valueFromDB(value: Any): MyCustomType = when(value) {
        is String -> MyCustomType.fromString(value)
        else -> error("Unexpected value of type ${value::class.qualifiedName}")
    }
    
    override fun notNullValueToDB(value: Any): Any = when(value) {
        is MyCustomType -> value.toString()
        else -> error("Unexpected value of type ${value::class.qualifiedName}")
    }
}

// Extensión para crear columnas con el tipo personalizado
fun Table.customType(name: String): Column<MyCustomType> = 
    registerColumn(name, MyCustomTypeColumnType())
```

## Resumen

La definición adecuada de tablas es crucial para trabajar eficientemente con Exposed. Este documento ha cubierto:

- Diferentes tipos de tablas para diversos escenarios
- Definición de columnas con varios tipos de datos
- Añadir restricciones como valores por defecto, claves primarias y foráneas
- Trabajar con índices y esquemas
- Crear tablas en la base de datos

Con estos conceptos, puedes modelar prácticamente cualquier esquema de base de datos de forma tipo segura en Kotlin utilizando Exposed.