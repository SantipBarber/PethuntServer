package com.pethunt.server.repositories

import com.pethunt.server.models.PetsTable
import com.pethunt.server.models.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object TestDatabaseFactory {
    
    fun init(): Database {
        val database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        
        transaction(database) {
            SchemaUtils.create(UsersTable, PetsTable)
        }
        
        return database
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}