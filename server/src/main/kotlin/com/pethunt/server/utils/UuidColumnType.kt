package com.pethunt.server.utils

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject
import java.sql.ResultSet
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tipo de columna personalizado para manejar UUID nativos de Kotlin con Exposed
 */
@OptIn(ExperimentalUuidApi::class)
class UuidColumnType : ColumnType() {
    override fun sqlType(): String = "UUID"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value is Uuid) {
            val pgObject = PGobject()
            pgObject.type = "uuid"
            pgObject.value = value.toString()
            stmt[index] = pgObject
        } else {
            stmt[index] = value
        }
    }

    override fun valueFromDB(value: Any): Uuid = when (value) {
        is String -> Uuid.fromString(value)
        is java.util.UUID -> Uuid.fromString(value.toString())
        is PGobject -> Uuid.fromString(value.value ?: throw IllegalArgumentException("UUID cannot be null"))
        else -> throw IllegalArgumentException("Unexpected value of type: ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Uuid -> value.toString()
        else -> value
    }
}

/**
 * Extensión de función para facilitar la creación de columnas UUID en tablas Exposed
 */
@OptIn(ExperimentalUuidApi::class)
fun Table.uuid(name: String): Column<Uuid> = registerColumn(name, UuidColumnType())