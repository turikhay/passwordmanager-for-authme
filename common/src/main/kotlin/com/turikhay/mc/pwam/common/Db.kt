package com.turikhay.mc.pwam.common

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.sql.Connection

fun initDb(): Database {
    val db = Database.connect(
        "jdbc:sqlite:file:pwam.db",
        "org.sqlite.JDBC",
    )
    val tm = db.transactionManager
    tm.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    transaction(db) {
        migrateIfNecessary()
    }
    return db
}