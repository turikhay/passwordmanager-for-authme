package com.turikhay.mc.pwam.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

private val logger = KotlinLogging.logger {}

object PasswordEntry : IntIdTable("password") {
    val username = varchar("username", 255)
    val server = varchar("server", 255)
    val password = varchar("password", 255)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val superseded = bool("superseded").default(false)
    val removed = bool("removed").default(false)
}

fun queryPassword(
    username: String,
    server: String,
) = PasswordEntry.selectAll().where {
    (PasswordEntry.username eq username) and
            (PasswordEntry.server eq server) and
            (PasswordEntry.superseded eq false) and
            (PasswordEntry.removed eq false)
}.orderBy(
    PasswordEntry.id to SortOrder.DESC
).firstOrNull()

fun getPassword(
    username: String,
    server: String,
): String? {
    return queryPassword(username, server)?.get(PasswordEntry.password)
}

fun setPassword(
    username: String,
    server: String,
    password: String,
) {
    logger.info { "Inserting the new $server password (user: $username)" }
    val now = System.currentTimeMillis()
    val id = PasswordEntry.insertAndGetId {
        it[PasswordEntry.username] = username
        it[PasswordEntry.server] = server
        it[PasswordEntry.password] = password
        it[createdAt] = now
        it[updatedAt] = now
    }
    val supersededCount = PasswordEntry.update({
        (PasswordEntry.id less id.value) and
                (PasswordEntry.username eq username) and
                (PasswordEntry.server eq server) and
                (PasswordEntry.removed eq false)
    }) {
        it[superseded] = true
        it[updatedAt] = now
    }
    logger.debug { "Superseded passwords count: $supersededCount" }
}