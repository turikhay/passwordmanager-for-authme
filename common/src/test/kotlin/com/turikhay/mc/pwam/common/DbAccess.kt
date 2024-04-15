package com.turikhay.mc.pwam.common

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.nio.file.Path

class DbAccess {
    lateinit var db: Database

    fun beforeAll(tempDir: Path) {
        db = Database.connect(
            "jdbc:sqlite:file:${tempDir.toAbsolutePath()}/test.db",
            "org.sqlite.JDBC",
        )
    }

    fun beforeEach() {
        db.transactionManager.run {
            bindTransactionToThread(newTransaction())
        }
    }

    fun afterAll() {
        db.transactionManager.currentOrNull()?.close()
    }
}