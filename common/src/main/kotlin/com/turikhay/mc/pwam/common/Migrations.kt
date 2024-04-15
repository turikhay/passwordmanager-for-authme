package com.turikhay.mc.pwam.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private val logger = KotlinLogging.logger {}

fun getLastMigrationId(): Int? {
    return transaction {
        val lastMigration = Migrations.selectAll()
            .orderBy(Migrations.id to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
        if (lastMigration == null) {
            logger.info { "There are no migrations in the database" }
            null
        } else {
            val id = lastMigration[Migrations.id].value
            logger.info { "Found last migration: $id" }
            id
        }
    }
}

object Migrations : IdTable<Int>("migration") {
    override val id = integer("id").entityId()
    val timestamp = long("timestamp")
}

fun interface Migration {
    fun migrate(transaction: Transaction)
}

val MIGRATIONS = listOf(
    Migration { _ ->
        logger.info { "Creating PasswordEntry" }
        SchemaUtils.create(PasswordEntry)
    }
)

fun migrateIfNecessary(migrations: List<Migration> = MIGRATIONS) {
    transaction {
        logger.debug { "Creating migrations table" }
        SchemaUtils.create(Migrations)
        val lastId = getLastMigrationId() ?: -1
        val skip = lastId + 1
        if (migrations.size == skip) {
            logger.info { "No migration is necessary" }
        } else {
            migrations.asSequence().drop(skip).forEachIndexed { index, migration ->
                val ogIndex = index + skip
                logger.info { "Executing migration $ogIndex" }
                migration.migrate(this)
                Migrations.insert {
                    it[id] = ogIndex
                    it[timestamp] = System.currentTimeMillis()
                }
            }
            logger.info { "Done executing migrations" }
        }
    }
}