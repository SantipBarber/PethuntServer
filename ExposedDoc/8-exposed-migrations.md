# Migración y evolución de esquemas en Exposed

El esquema de una base de datos rara vez permanece estático a lo largo del tiempo. A medida que una aplicación evoluciona, es necesario añadir tablas, modificar columnas, crear índices o reestructurar relaciones. Este documento explica las herramientas y estrategias disponibles en Exposed para gestionar la evolución de esquemas de bases de datos.

## Operaciones básicas de esquema

### SchemaUtils

La clase `SchemaUtils` proporciona métodos para crear, modificar y eliminar objetos de la base de datos.

#### Crear tablas

```kotlin
transaction {
    SchemaUtils.create(Users, Posts, Comments)
}
```

Este método crea las tablas especificadas si no existen. Los objetos `Users`, `Posts` y `Comments` son tus definiciones de tablas que extienden de `Table`.

#### Crear tablas y columnas faltantes

Para esquemas existentes, puedes detectar y crear solo las tablas y columnas que faltan:

```kotlin
transaction {
    SchemaUtils.createMissingTablesAndColumns(Users, Posts, Comments)
}
```

Este método es útil para desarrollo y para añadir nuevas columnas o tablas a un esquema existente sin tener que eliminar los datos.

#### Eliminar tablas

```kotlin
transaction {
    SchemaUtils.drop(Comments, Posts, Users)
}
```

Este método elimina las tablas especificadas. Nota que el orden importa: si tienes restricciones de clave foránea, debes eliminar las tablas dependientes primero (como se muestra en el ejemplo, donde `Comments` depende de `Posts` que a su vez depende de `Users`).

#### Eliminar y crear tablas

Un método conveniente para recrear tablas desde cero:

```kotlin
transaction {
    SchemaUtils.drop(Comments, Posts, Users)
    SchemaUtils.create(Users, Posts, Comments)
}
```

O más simple:

```kotlin
transaction {
    SchemaUtils.dropCreate(Users, Posts, Comments)
}
```

#### Crear y eliminar índices y restricciones

```kotlin
transaction {
    // Crear índices
    SchemaUtils.createIndex(IndexA, IndexB)
    
    // Eliminar índices
    SchemaUtils.dropIndex(IndexA, IndexB)
    
    // Crear restricciones de clave foránea
    SchemaUtils.createFKey(FKeyA, FKeyB)
    
    // Eliminar restricciones de clave foránea
    SchemaUtils.dropFKey(FKeyA, FKeyB)
}
```

## Gestión detallada de migraciones

Para aplicaciones en producción, normalmente necesitas un enfoque más controlado para las migraciones.

### Verificación de diferencias de esquema

Puedes verificar si hay diferencias entre tu modelo y la base de datos:

```kotlin
transaction {
    // Detectar índices excesivos (definidos en la BD pero no en tus modelos)
    val excessiveIndices = SchemaUtils.checkExcessiveIndices(Users, Posts) 
    println("Índices excesivos: $excessiveIndices")
    
    // Detectar restricciones de clave foránea excesivas
    val excessiveFKeys = SchemaUtils.checkExcessiveForeignKeys(Users, Posts)
    println("Claves foráneas excesivas: $excessiveFKeys")
    
    // Generar sentencias SQL para la migración
    val statements = SchemaUtils.statementsRequiredToActualizeScheme(Users, Posts)
    statements.forEach { println(it) }
}
```

### Exponiendo el módulo de migración

Para migraciones más sofisticadas, Exposed ofrece el módulo `exposed-migration`. Añádelo a tu proyecto:

```kotlin
implementation("org.jetbrains.exposed:exposed-migration:$exposedVersion")
```

Este módulo proporciona la clase `MigrationUtils` con métodos avanzados para gestionar migraciones.

```kotlin
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.migration.MigrationUtils

transaction {
    // Generar sentencias SQL para migrar de modelos antiguos a nuevos
    val statements = MigrationUtils.statementsRequiredForDatabaseMigration(
        oldTables = listOf(OldUsers, OldPosts),
        newTables = listOf(Users, Posts)
    )
    
    // Ejecutar las sentencias
    statements.forEach { statement ->
        exec(statement)
    }
}
```

## Implementando una estrategia de migración

Para aplicaciones en producción, es recomendable implementar una estrategia formal de migración.

### 1. Sistema de versiones para migraciones

Define un sistema para gestionar y aplicar migraciones:

```kotlin
data class Migration(
    val version: Int,
    val description: String,
    val script: Transaction.() -> Unit
)

object Migrations {
    private val migrations = mutableListOf<Migration>()
    
    fun register(version: Int, description: String, script: Transaction.() -> Unit) {
        migrations.add(Migration(version, description, script))
    }
    
    fun migrateToLatest(database: Database) {
        transaction(database) {
            // Crear tabla para rastrear versiones si no existe
            SchemaUtils.create(MigrationTable)
            
            // Obtener la versión actual
            val currentVersion = MigrationTable.select(MigrationTable.version.max())
                .singleOrNull()?.getOrNull(MigrationTable.version.max()) ?: 0
            
            // Aplicar migraciones pendientes en orden
            migrations.filter { it.version > currentVersion }
                .sortedBy { it.version }
                .forEach { migration ->
                    println("Aplicando migración v${migration.version}: ${migration.description}")
                    migration.script(this)
                    
                    // Registrar la migración aplicada
                    MigrationTable.insert {
                        it[version] = migration.version
                        it[appliedAt] = LocalDateTime.now()
                        it[description] = migration.description
                    }
                    
                    println("Migración v${migration.version} aplicada correctamente")
                }
        }
    }
}

// Tabla para registrar migraciones
object MigrationTable : Table("migrations") {
    val version = integer("version").uniqueIndex()
    val appliedAt = datetime("applied_at")
    val description = varchar("description", 200)
    
    override val primaryKey = PrimaryKey(version)
}
```

### 2. Definición de migraciones

Define las migraciones individuales:

```kotlin
// Registrar migraciones
Migrations.register(1, "Crear tablas iniciales") {
    SchemaUtils.create(Users, Posts)
}

Migrations.register(2, "Añadir tabla de comentarios") {
    SchemaUtils.create(Comments)
}

Migrations.register(3, "Añadir columna 'active' a usuarios") {
    exec("ALTER TABLE users ADD COLUMN active BOOLEAN DEFAULT true")
}

Migrations.register(4, "Crear índices para búsqueda") {
    SchemaUtils.createIndex(
        Index(
            name = "idx_posts_title", 
            columns = listOf(Posts.title),
            unique = false
        )
    )
}
```

### 3. Aplicar migraciones al inicio

Aplica las migraciones al arrancar tu aplicación:

```kotlin
fun main() {
    // Configurar base de datos
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/mydb",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "password"
    )
    
    // Aplicar migraciones
    Migrations.migrateToLatest(database)
    
    // Continuar con la inicialización de la aplicación
    startApplication()
}
```

## Estrategias de migración para diferentes escenarios

### Migraciones para desarrollo

Durante el desarrollo, a menudo necesitas un enfoque más flexible:

```kotlin
fun setupDevDatabase() {
    transaction {
        // En desarrollo, podemos recrear el esquema
        SchemaUtils.drop(Comments, Posts, Users)
        SchemaUtils.create(Users, Posts, Comments)
        
        // Cargar datos de prueba
        loadTestData()
    }
}
```

### Migraciones para producción

En producción, se necesita un enfoque más conservador:

```kotlin
fun setupProdDatabase() {
    transaction {
        // En producción, solo añadir lo que falta
        SchemaUtils.createMissingTablesAndColumns(Users, Posts, Comments)
        
        // Ejecutar migraciones específicas que no pueden ser manejadas automáticamente
        runCustomMigrations()
    }
}

fun runCustomMigrations() {
    // Verificar si necesitamos migrar
    val needsToRunCustomMigration = exec("SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'old_column'") { rs ->
        rs.next() && rs.getLong(1) > 0
    } ?: false
    
    if (needsToRunCustomMigration) {
        // Migrar datos
        exec("UPDATE users SET new_column = old_column")
        // Eliminar columna antigua (si la base de datos lo soporta)
        exec("ALTER TABLE users DROP COLUMN old_column")
    }
}
```

## Migraciones complejas

### Renombrar columnas

La mayoría de las bases de datos no tienen un comando directo para renombrar columnas, por lo que debes usar un enfoque de varias etapas:

```kotlin
fun renameColumn() {
    transaction {
        // 1. Añadir nueva columna
        exec("ALTER TABLE users ADD COLUMN new_email VARCHAR(100)")
        
        // 2. Copiar datos
        exec("UPDATE users SET new_email = email")
        
        // 3. Eliminar la columna antigua
        exec("ALTER TABLE users DROP COLUMN email")
        
        // 4. Renombrar la nueva columna (en bases de datos que lo soportan)
        exec("ALTER TABLE users RENAME COLUMN new_email TO email")
    }
}
```

### Cambiar tipos de datos

Para cambiar el tipo de una columna:

```kotlin
fun changeColumnType() {
    transaction {
        // Para PostgreSQL
        exec("ALTER TABLE posts ALTER COLUMN views TYPE BIGINT")
        
        // Para MySQL
        exec("ALTER TABLE posts MODIFY COLUMN views BIGINT")
        
        // Para SQLite (requiere recrear la tabla, simplificado)
        exec("ALTER TABLE posts RENAME TO posts_old")
        SchemaUtils.create(PostsNewTable)
        exec("INSERT INTO posts SELECT id, title, content, CAST(views AS BIGINT), user_id, created_at FROM posts_old")
        exec("DROP TABLE posts_old")
    }
}
```

### Migración de datos

A veces necesitas transformar datos durante una migración:

```kotlin
fun migrateData() {
    transaction {
        // Crear nueva estructura
        SchemaUtils.create(NewUsers)
        
        // Migrar datos con transformación
        exec("""
            INSERT INTO new_users (id, full_name, email, age)
            SELECT id, 
                   CONCAT(first_name, ' ', last_name) as full_name, 
                   email,
                   EXTRACT(YEAR FROM AGE(NOW(), birth_date)) as age
            FROM users
        """)
        
        // Eliminar estructura antigua
        SchemaUtils.drop(OldUsers)
        
        // Renombrar tabla nueva (específico de base de datos)
        exec("ALTER TABLE new_users RENAME TO users")
    }
}
```

## Integración con bibliotecas externas de migración

Si necesitas una solución más robusta, puedes integrar Exposed con bibliotecas específicas para migración:

### Flyway

```kotlin
fun setupWithFlyway() {
    // Configurar Flyway
    val flyway = Flyway.configure()
        .dataSource("jdbc:postgresql://localhost:5432/mydb", "postgres", "password")
        .locations("classpath:db/migration")
        .load()
    
    // Aplicar migraciones
    flyway.migrate()
    
    // Luego conectar Exposed a la misma base de datos
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/mydb",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "password"
    )
    
    // Ahora puedes usar Exposed con un esquema actualizado
}
```

Con Flyway, guardarías los scripts SQL de migración en `src/main/resources/db/migration` con nombres como `V1__Create_users_table.sql`.

### Liquibase

```kotlin
fun setupWithLiquibase() {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
        username = "postgres"
        password = "password"
    })
    
    // Aplicar cambios con Liquibase
    val database = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(JdbcConnection(dataSource.connection))
    
    val liquibase = Liquibase(
        "classpath:db/changelog/db.changelog-master.xml",
        ClassLoaderResourceAccessor(),
        database
    )
    
    liquibase.update("")
    
    // Conectar Exposed a la misma base de datos
    Database.connect(dataSource)
}
```

## Evolución de esquemas con la API DAO

Si estás usando la API DAO de Exposed, ten en cuenta estas consideraciones adicionales:

### Evolución de clases de entidad

Cuando cambias una entidad DAO, asegúrate de mantener la compatibilidad:

```kotlin
// Versión 1
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var name by UsersTable.name
    var email by UsersTable.email
}

// Versión 2 (después de migración)
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UsersTable)
    
    var firstName by UsersTable.firstName  // Columna renombrada
    var lastName by UsersTable.lastName    // Nueva columna
    var email by UsersTable.email
    
    // Propiedad de compatibilidad
    var name: String
        get() = "$firstName $lastName"
        set(value) {
            val parts = value.split(" ", limit = 2)
            firstName = parts[0]
            lastName = parts.getOrElse(1) { "" }
        }
}
```

## Mejores prácticas para migraciones

1. **Idempotencia**: Las migraciones deben ser idempotentes (pueden ejecutarse múltiples veces sin efectos secundarios).
   
   ```kotlin
   fun createIndexIfNotExists() {
       transaction {
           val indexExists = exec("SELECT COUNT(*) FROM information_schema.statistics WHERE table_name = 'posts' AND index_name = 'idx_posts_title'") { rs ->
               rs.next() && rs.getLong(1) > 0
           } ?: false
           
           if (!indexExists) {
               SchemaUtils.createIndex(
                   Index(name = "idx_posts_title", columns = listOf(Posts.title))
               )
           }
       }
   }
   ```

2. **Control de versiones**: Mantén un registro de las migraciones aplicadas.

3. **Migraciones atómicas**: Cada migración debe hacer una sola cosa lógica.

4. **Scripts de reversión**: Para cada migración, considera tener un script de reversión.
   
   ```kotlin
   Migrations.register(
       version = 5,
       description = "Añadir columna 'status' a Posts",
       script = {
           exec("ALTER TABLE posts ADD COLUMN status VARCHAR(20) DEFAULT 'DRAFT'")
       },
       revert = {
           exec("ALTER TABLE posts DROP COLUMN status")
       }
   )
   ```

5. **Pruebas**: Prueba las migraciones en ambientes de desarrollo antes de aplicarlas en producción.

6. **Backups**: Siempre realiza un backup antes de aplicar migraciones en producción.

7. **Transacciones**: Ejecuta las migraciones dentro de transacciones cuando sea posible para poder hacer rollback si algo falla.

## Ejemplo completo: Implementación de migraciones

Aquí hay un ejemplo más completo que incorpora muchas de las mejores prácticas:

```kotlin
// Definición de migración
data class Migration(
    val version: Int,
    val description: String,
    val script: Transaction.() -> Unit,
    val revert: Transaction.() -> Unit
)

// Tabla para rastrear migraciones
object MigrationTable : Table("migrations") {
    val version = integer("version").uniqueIndex()
    val appliedAt = datetime("applied_at")
    val description = varchar("description", 200)
    
    override val primaryKey = PrimaryKey(version)
}

// Gestor de migraciones
class MigrationManager(private val database: Database) {
    private val migrations = mutableListOf<Migration>()
    
    fun register(
        version: Int,
        description: String,
        script: Transaction.() -> Unit,
        revert: Transaction.() -> Unit
    ) {
        migrations.add(Migration(version, description, script, revert))
    }
    
    fun migrateToLatest() {
        transaction(database) {
            // Asegurar que la tabla de migraciones existe
            SchemaUtils.create(MigrationTable)
            
            // Obtener la versión actual
            val currentVersion = MigrationTable
                .slice(MigrationTable.version.max())
                .selectAll()
                .singleOrNull()
                ?.getOrNull(MigrationTable.version.max()) ?: 0
            
            // Aplicar migraciones pendientes en orden
            migrations
                .filter { it.version > currentVersion }
                .sortedBy { it.version }
                .forEach { migration ->
                    try {
                        println("Aplicando migración v${migration.version}: ${migration.description}")
                        
                        // Ejecutar la migración
                        migration.script(this)
                        
                        // Registrar migración exitosa
                        MigrationTable.insert {
                            it[version] = migration.version
                            it[appliedAt] = LocalDateTime.now()
                            it[description] = migration.description
                        }
                        
                        println("Migración v${migration.version} aplicada correctamente")
                    } catch (e: Exception) {
                        println("Error al aplicar migración v${migration.version}: ${e.message}")
                        rollback()
                        throw e
                    }
                }
        }
    }
    
    fun revertToVersion(targetVersion: Int) {
        transaction(database) {
            // Obtener migraciones aplicadas en orden descendente
            val appliedVersions = MigrationTable
                .slice(MigrationTable.version)
                .selectAll()
                .map { it[MigrationTable.version] }
                .sortedDescending()
            
            // Buscar migraciones a revertir
            for (version in appliedVersions) {
                if (version <= targetVersion) break
                
                val migration = migrations.find { it.version == version }
                if (migration == null) {
                    println("¡Advertencia! No se encontró script de reversión para v$version")
                    continue
                }
                
                try {
                    println("Revirtiendo migración v${migration.version}: ${migration.description}")
                    
                    // Ejecutar script de reversión
                    migration.revert(this)
                    
                    // Eliminar registro de migración
                    MigrationTable.deleteWhere { MigrationTable.version eq version }
                    
                    println("Migración v${migration.version} revertida correctamente")
                } catch (e: Exception) {
                    println("Error al revertir migración v${migration.version}: ${e.message}")
                    rollback()
                    throw e
                }
            }
        }
    }
}

// Uso del gestor de migraciones
fun main() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/mydb",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "password"
    )
    
    val migrationManager = MigrationManager(database)
    
    // Registrar migraciones
    migrationManager.register(
        version = 1,
        description = "Crear tabla de usuarios",
        script = {
            SchemaUtils.create(UsersTable)
        },
        revert = {
            SchemaUtils.drop(UsersTable)
        }
    )
    
    migrationManager.register(
        version = 2,
        description = "Crear tabla de posts",
        script = {
            SchemaUtils.create(PostsTable)
        },
        revert = {
            SchemaUtils.drop(PostsTable)
        }
    )
    
    migrationManager.register(
        version = 3,
        description = "Añadir columna 'active' a usuarios",
        script = {
            exec("ALTER TABLE users ADD COLUMN active BOOLEAN DEFAULT true")
        },
        revert = {
            exec("ALTER TABLE users DROP COLUMN active")
        }
    )
    
    // Aplicar migraciones
    migrationManager.migrateToLatest()
    
    // O revertir a una versión específica
    // migrationManager.revertToVersion(1)
}
```

## Resumen

Una estrategia robusta de migración de esquema es esencial para cualquier aplicación que evolucione con el tiempo. Exposed proporciona herramientas útiles como `SchemaUtils` y `MigrationUtils` para facilitar este proceso.

Los puntos clave a recordar:

1. Para desarrollo y prototipos rápidos, `SchemaUtils.create()` y `SchemaUtils.createMissingTablesAndColumns()` pueden ser suficientes.

2. Para aplicaciones en producción, implementa un sistema formal de migraciones con control de versiones, como el ejemplo mostrado.

3. Considera herramientas específicas como Flyway o Liquibase para escenarios de migración más complejos.

4. Sigue las mejores prácticas: idempotencia, atomicidad, pruebas y backups.

Con una estrategia adecuada de migración, puedes evolucionar el esquema de tu base de datos de manera segura y confiable a medida que tu aplicación crece y cambia.