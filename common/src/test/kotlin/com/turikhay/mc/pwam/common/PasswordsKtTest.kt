package com.turikhay.mc.pwam.common

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PasswordsKtTest {
    @Test
    @Order(1)
    fun `returns no password`() {
        transaction {
            assertNull(getPassword("foo", "bar"))
        }
    }

    @Test
    @Order(2)
    fun `adds password`() {
        transaction {
            setPassword("foo", "bar", "1234")
            assertEquals("1234", getPassword("foo", "bar"))
        }
    }

    @Test
    @Order(3)
    fun `updates password`() {
        transaction {
            setPassword("foo", "bar", "12345")
            assertEquals("12345", getPassword("foo", "bar"))
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
            transaction(dbAccess.db) {
                migrateIfNecessary()
            }
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