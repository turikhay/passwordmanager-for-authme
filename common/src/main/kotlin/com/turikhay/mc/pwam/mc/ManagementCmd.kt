package com.turikhay.mc.pwam.mc

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.turikhay.mc.pwam.common.*
import com.turikhay.mc.pwam.common.andWhere
import com.turikhay.mc.pwam.mc.BroadStringArgumentType.Companion.broadString
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.apache.commons.lang3.StringUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private const val PER_PAGE = 10
private const val ALL_USERS = "@allusers"
private const val ALL_SERVERS = "@allservers"
private const val HERE = "@here"

private const val COLUMN_IDX = "index"
private const val COLUMN_USERNAME = "username"
private const val COLUMN_SERVER = "server"
private const val COLUMN_PWD = "password"
private const val COLUMN_SET = "set"
private const val COLUMN_REMOVE = "remove"
private val COLUMN_LIST = listOf(
    COLUMN_IDX,
    COLUMN_USERNAME,
    COLUMN_SERVER,
    COLUMN_PWD,
    COLUMN_SET,
    COLUMN_REMOVE,
)

class ManagementCmd<C>(
    private val db: Database,
    private val patternFactory: PasswordPattern,
    private val sessionInfo: SessionInfo,
    private val audience: PlatformAudience,
    private val passwordChangeCallback: PasswordChangeCallback,
    private val version: String = "0.0.0-SNAPSHOT",
) {

    private inner class ListPasswords<C>(
        ctx: CommandContext<C>
    ) {
        val pageArg = ctx.getArgumentSafely("page", Integer::class.java)?.toInt() ?: 1
        val usernameArg0 = ctx.getArgumentSafely("username")
        val usernameArg = usernameArg0.let {
            when(it) {
                ALL_USERS -> null
                null -> sessionInfo.username
                else -> it
            }
        }
        val serverArg0 = ctx.getArgumentSafely("server")
        val serverArg = serverArg0.let {
            when(it) {
                ALL_SERVERS -> null
                HERE, null -> sessionInfo.server
                else -> it
            }
        }
        val offset = (pageArg.toLong() - 1) * PER_PAGE
        var pagesTotal: Long = 0

        fun run() {
            var rows: List<PwdRow> = emptyList()
            transaction(db) {
                val count = PasswordEntry.selectAll().setWhere().count()
                if (count == 0L) {
                    audience.sendMessage(
                        text().append(
                            text("No passwords found")
                        )
                    )
                    return@transaction
                }
                pagesTotal = (count + PER_PAGE - 1) / PER_PAGE
                if (pageArg > pagesTotal) {
                    audience.sendMessage(
                        text().append(
                            text("There is no such page ($pageArg/$pagesTotal)")
                        )
                    )
                    return@transaction
                }
                rows = collectRows()
            }
            audience.sendMessage(text())
            audience.sendMessage(
                text().append(
                    text("Listing passwords of ${usernameArg ?: "all users"} on ${serverArg ?: "all servers"} (page $pageArg/$pagesTotal)")
                )
            )
            audience.sendMessage(text())
            for (row in rows) {
                var rowText = text()
                for (columnName in COLUMN_LIST) {
                    val columnText = row.cols[columnName] ?: continue
                    rowText = rowText.append(text()
                        .append(columnText)
                        .append(text("  "))
                    )
                }
                audience.sendMessage(rowText)
            }
            audience.sendMessage(text())
            audience.sendMessage(createPaginatorButtons())
        }

        private fun collectRows(): List<PwdRow> {
            val currentPwdId = queryPassword(
                sessionInfo.username,
                sessionInfo.server,
            )?.get(PasswordEntry.id)
            val query = PasswordEntry
                .select(
                    PasswordEntry.id,
                    PasswordEntry.username,
                    PasswordEntry.password,
                    PasswordEntry.server,
                    PasswordEntry.removed,
                    PasswordEntry.superseded,
                )
                .limit(PER_PAGE, offset)
                .orderBy(
                    PasswordEntry.updatedAt to SortOrder.DESC,
                    PasswordEntry.superseded to SortOrder.ASC,
                )
                .setWhere()
            return query.mapIndexed { index, row ->
                PwdRow(
                    index,
                    row,
                    row[PasswordEntry.id] == currentPwdId,
                )
            }
        }

        private fun createPaginatorButtons(): TextComponent {
            val changePageBuilder = StringBuilder()
            changePageBuilder.append("/").append(LABEL).append(" list")
            if (serverArg0 != null) {
                changePageBuilder.append(" ").append(serverArg0)
            }
            if (usernameArg0 != null) {
                changePageBuilder.append(" ").append(usernameArg0)
            }
            val changePageBase = String(changePageBuilder)
            val pagination = text()
            fun Component.pageTextButton(
                enabled: Boolean,
                delta: Int,
            ) = if (enabled) {
                style(
                    Style.style().decorate(TextDecoration.UNDERLINED)
                ).clickEvent(
                    ClickEvent.runCommand("$changePageBase ${pageArg + delta}")
                )
            } else {
                style { it.color(NamedTextColor.GRAY) }
            }
            val len = pagesTotal.toString().length
            pagination
                .append(
                    text("[«]").pageTextButton(
                        pageArg > 1,
                        -1,
                    )
                )
                .append(
                    text()
                        .append(text(" "))
                        .append(text(StringUtils.rightPad(pageArg.toString(), len)))
                        .append(text(" / "))
                        .append(text(StringUtils.rightPad(pagesTotal.toString(), len)))
                        .append(text(" "))
                )
            pagination.append(
                text("[»]").pageTextButton(
                    pageArg < pagesTotal,
                    1,
                )
            )
            return pagination.build()
        }

        private fun Query.setWhere(): Query {
            andWhere { PasswordEntry.removed eq false }
            if (usernameArg != null) {
                andWhere { PasswordEntry.username eq usernameArg }
            }
            if (serverArg != null) {
                andWhere { Op.build { PasswordEntry.server eq serverArg } }
            }
            return this@setWhere
        }

        inner class PwdRow(
            val index: Int,
            val row: ResultRow,
            val isCurrentPwd: Boolean
        ) {
            val cols = run {
                val r = mutableMapOf<String, TextComponent>()
                r[COLUMN_IDX] = text("${offset + index + 1}.")
                if (usernameArg == null) {
                    val username = row[PasswordEntry.username]
                    r[COLUMN_USERNAME] = text()
                        .append(
                            text(username)
                                .color(
                                    seedTextColor(username)
                                )
                        )
                        .let {
                            if (username == sessionInfo.username) {
                                it.append(text(" (you)"))
                            } else {
                                it
                            }
                        }
                        .build()
                }
                if (serverArg == null) {
                    val server = row[PasswordEntry.server]
                    r[COLUMN_SERVER] = text(server)
                        .color(
                            seedTextColor(server)
                        )
                        .hoverEvent(
                            HoverEvent.showText(text("Click to copy server address"))
                        )
                        .clickEvent(
                            ClickEvent.copyToClipboard(server)
                        )
                }
                val password = row[PasswordEntry.password]
                r[COLUMN_PWD] = patternFactory.compPatternOf(password)
                r[COLUMN_SET] = text("[✔]").let {
                    if (isCurrentPwd) {
                        it.hoverEvent(
                            HoverEvent.showText(text("This is your current password on this server"))
                        ).style { s ->
                            s.color(NamedTextColor.GRAY)
                        }
                    } else {
                        it.hoverEvent(
                            HoverEvent.showText(text("Set as current password"))
                        )
                        .clickEvent(
                            ClickEvent.suggestCommand("/$LABEL set $password")
                        )
                    }
                }
                r[COLUMN_REMOVE] = run {
                    text("[x]")
                        .hoverEvent(
                            HoverEvent.showText(text("Remove the password"))
                        )
                        .clickEvent(
                            ClickEvent.suggestCommand("/$LABEL removeById ${row[PasswordEntry.id]}")
                        )
                }
                r
            }
        }
    }

    fun listPasswords(
        ctx: CommandContext<C>,
    ) = ListPasswords(ctx).run()

    fun suggestServerList(ctx: CommandContext<C>): List<String> {
        val serverArg = ctx.getArgumentSafely("server")
        return suggest(PasswordEntry.server, serverArg, listOf(HERE, ALL_SERVERS))
    }

    fun suggestUsernames(ctx: CommandContext<C>): List<String> {
        val usernameArg = ctx.getArgumentSafely("username")
        return suggest(PasswordEntry.username, usernameArg, listOf(ALL_USERS))
    }

    fun setAsCurrent(ctx: CommandContext<C>) {
        val password = ctx.getArgument("password")
        passwordChangeCallback.changePassword(password)
    }

    fun removeById(ctx: CommandContext<C>) {
        val id = ctx.getArgument("id", Integer::class.java).toInt()
        transaction(db) {
            val count = PasswordEntry.update(
                where = {
                    (PasswordEntry.id eq id) and (
                            PasswordEntry.removed eq false
                    )
                },
                body = { it[removed] = true }
            )
            if (count > 0) {
                audience.sendMessage(
                    text()
                        .append(
                            text(
                                "Password removed successfully",
                                NamedTextColor.GREEN
                            )
                        )
                )
            } else {
                audience.sendMessage(
                    text()
                        .append(
                            text(
                                "Password not found",
                                NamedTextColor.RED
                            )
                        )
                )
            }
        }
    }

    private fun suggest(column: Column<String>, argument: String?, placeholders: List<String>): List<String> {
        val list = mutableListOf<String>()
        transaction(db) {
            val query = PasswordEntry
                .select(column)
                .withDistinct(true)
                .limit(5)
                .orderBy(PasswordEntry.updatedAt to SortOrder.DESC)
                .where {
                    (PasswordEntry.removed eq false) and (PasswordEntry.superseded eq false)
                }
            if (argument != null) {
                query.andWhere { column.like("$argument%") }
            }
            query.forEach { row ->
                list.add(row[column])
            }
        }
        if (argument == null) {
            list.addAll(placeholders)
        } else {
            list.addAll(
                placeholders.asSequence().filter { it.startsWith(argument) }
            )
        }
        return list
    }

    fun about() {
        audience.sendMessage(
            text()
                .append(text("[PWAM]", NamedTextColor.RED))
                .append(space())
                .append(text("Version: $version"))
        )
    }

    companion object {
        const val LABEL = "pwam"
        fun setup(
            dispatcher: CommandDispatcher<ICommandSource>,
            platformDispatcher: PlatformCommandDispatcher,
            executor: Executor,
            cmd: ManagementCmd<ICommandSource>,
        ) {
            fun <C, T: ArgumentBuilder<C, T>> T.executesLater(cmd: (CommandContext<C>) -> Unit): T {
                return executes { ctx ->
                    platformDispatcher.addCommandToHistory(ctx.input)
                    executor.execute {
                        cmd(ctx)
                    }
                    0
                }
            }
            val pageParam =
                argument<ICommandSource, Int>("page", integer(1))
                    .executesLater(cmd::listPasswords)
            val usernameParam = argument<ICommandSource, String>("username", broadString())
                .executesLater(cmd::listPasswords)
                .suggestsLater(executor, cmd::suggestUsernames)
                .then(pageParam)
            dispatcher.register(
                literal<ICommandSource>(LABEL)
                    .executesLater { cmd.about() }
                    .then(
                        literal<ICommandSource>("list")
                            .executesLater(cmd::listPasswords)
                            .then(
                                argument<ICommandSource, String>("server", broadString())
                                    .executesLater(cmd::listPasswords)
                                    .suggestsLater(executor, cmd::suggestServerList)
                                    .then(pageParam)
                                    .then(usernameParam)
                            )
                    )
                    .then(
                        literal<ICommandSource>("set")
                            .then(
                                argument<ICommandSource, String>("password", broadString())
                                    .executesLater(cmd::setAsCurrent)
                            )
                    )
                    .then(
                        literal<ICommandSource>("removeById")
                            .then(
                                argument<ICommandSource, Int>("id", integer())
                                    .executesLater(cmd::removeById)
                            )
                    )
            )
        }
    }
}

private fun <C, V, T: RequiredArgumentBuilder<C, V>> T.suggestsLater(
    executor: Executor,
    provider: (CommandContext<C>) -> List<String>
): RequiredArgumentBuilder<C, V> {
    return suggests { ctx, builder ->
        val f = CompletableFuture<Suggestions>()
        executor.execute {
            val suggestions = provider(ctx)
            suggestions.forEach { builder.suggest(it) }
            f.complete(builder.build())
        }
        f
    }
}
