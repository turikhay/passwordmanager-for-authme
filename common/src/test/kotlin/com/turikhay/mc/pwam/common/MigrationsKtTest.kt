package com.turikhay.mc.pwam.common

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MigrationsKtTest {
    @Test
    @Order(1)
    fun `migrates empty database`() {
        transaction {
            migrateIfNecessary()
        }
    }

    @Test
    @Order(2)
    fun `migration creates an entry`() {
        transaction {
            assertEquals(
                MIGRATIONS.size - 1,
                getLastMigrationId()
            )
        }
    }

    companion object {
        @TempDir
        lateinit var tempDir: Path
        private val dbAccess = DbAccess()

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            dbAccess.beforeAll(this.tempDir)
        }

        @BeforeEach
        fun beforeEach() {
            dbAccess.beforeAll(tempDir)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            dbAccess.afterAll()
        }
    }
}