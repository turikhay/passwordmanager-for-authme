package com.turikhay.mc.pwam.mc

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.turikhay.mc.pwam.common.text.Pairs
import com.turikhay.mc.pwam.common.text.PasswordPairProvider
import com.turikhay.mc.pwam.common.text.TextProvider
import com.turikhay.mc.pwam.mc.BroadStringArgumentType.Companion.broadString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

class CommandDispatcherHandler(
    private val handler: KnownCommandHandler,
    private val pairs: Pairs,
    private val nodeAccessor: ICommandNodeAccessor<ICommandSource>,
    private val askServerSuggestion: IAskServerSuggestion,
    private val managementCmd: ManagementCmd<ICommandSource>,
    private val platformCommandDispatcher: PlatformCommandDispatcher,
    private val executor: Executor,
    private val dbFuture: CompletableFuture<Database>,
) {
    private lateinit var plugin: KnownPlugin
    private val labels = ArrayList<String>()
    private val commands = ArrayList<CommandNode<ICommandSource>>()
    private lateinit var customDispatcher: CommandDispatcher<ICommandSource>

    fun isKnownCommand(command: String): Boolean {
        val cmd = command.split(' ', limit = 2)
        if (!labels.contains(cmd[0])) {
            logger.info { "unknown label: ${cmd[0]}" }
            return false
        }
        return true
    }

    fun dispatchKnownCommand(command: String, source: ICommandSource) {
        logger.debug { "dispatching command: $command" }
        executor.execute {
            try {
                customDispatcher.execute(command, source)
            } catch (syntax: CommandSyntaxException) {
                logger.error(syntax) { "Failed to execute the following command: $command" }
                platformCommandDispatcher.dispatchCommandAndAddToHistory(command)
            }
        }
    }

    fun onCommandTreeReceived(vanillaDispatcher: CommandDispatcher<ICommandSource>) {
        plugin = detectPlugin(vanillaDispatcher)
        labels.clear()
        commands.clear()
        customDispatcher = CommandDispatcher()
        registerSelfCommand(vanillaDispatcher)
        registerPairCommands(
            LOGIN_COMMANDS,
            vanillaDispatcher,
            pairs.login,
            listOf("password"),
            handler::onLoginCommand,
        )
        registerPairCommands(
            REGISTER_COMMANDS,
            vanillaDispatcher,
            pairs.register,
            listOf("password", "password-again"),
            handler::onRegisterCommand,
        )
        registerCommands(
            CHANGE_PASSWORD_COMMANDS,
            vanillaDispatcher,
            buildCmdArgs(
                mapOf(
                    "old-password" to pairs.login.pattern,
                    "new-password" to pairs.register.pattern,
                ),
                handler::onChangePasswordCommand,
            ),
        )
        registerCommands(
            UNREGISTER_COMMANDS,
            vanillaDispatcher,
            buildCmdArgs(
                mapOf(
                    "password" to pairs.login.pattern,
                ),
                handler::onUnregisterCommand,
            ),
        )
    }

    private fun registerSelfCommand(vanillaDispatcher: CommandDispatcher<ICommandSource>) {
        labels.add(ManagementCmd.LABEL)
        listOf(
            vanillaDispatcher,
            customDispatcher,
        ).forEach { dispatcher ->
            ManagementCmd.setup(
                dispatcher,
                platformCommandDispatcher,
                executor,
                managementCmd,
                dbFuture,
            )
        }
    }

    private fun detectPlugin(dispatcher: CommandDispatcher<ICommandSource>): KnownPlugin {
        for (child in dispatcher.root.children) {
            for (plugin in KnownPlugin.entries) {
                if (child.name.startsWith(plugin.commandPrefix + ":")) {
                    logger.info { "Detected plugin: $plugin" }
                    return plugin
                }
            }
        }
        val fallback = KnownPlugin.AUTHME
        logger.info { "Couldn't detect login plugin, will assume it's $fallback" }
        return fallback
    }

    private fun registerCommands(
        names: List<String>,
        vanillaDispatcher: CommandDispatcher<ICommandSource>,
        commandArgs: RequiredArgumentBuilder<ICommandSource, String>,
    ) {
        val processed = processVanillaDispatcher(
            vanillaDispatcher,
            names,
            commandArgs.build(),
        )
        labels.addAll(processed)
        for (label in processed) {
            customDispatcher.register(
                literal<ICommandSource>(label)
                    .then(commandArgs)
            )
        }
    }

    private fun registerPairCommands(
        names: List<String>,
        vanillaDispatcher: CommandDispatcher<ICommandSource>,
        pairProvider: PasswordPairProvider,
        argNames: List<String>,
        executor: CommandExecutor,
    ) {
        return registerCommands(
            names,
            vanillaDispatcher,
            buildCmdArgs(
                argNames.associateWith {
                    pairProvider.pattern
                },
                executor,
            ),
        )
    }

    private fun processVanillaDispatcher(
        vanillaDispatcher: CommandDispatcher<ICommandSource>,
        names: List<String>,
        augmentedNode: CommandNode<ICommandSource>,
    ): List<String> {
        val processed = ArrayList<String>()
        for (name in names) {
            val node = vanillaDispatcher.root.getChild(name)
            if (node == null) {
                logger.info { "Command doesn't exist: $name" }
                continue
            }
            logger.info { "Processing command: $node" }
            val children = nodeAccessor.getChildren(node)
            if (children.isNotEmpty()) {
                val i = children.iterator()
                while (i.hasNext()) {
                    val entry = i.next()
                    logger.info { "Processing entry: $entry" }
                    val value = entry.value
                    if (value is ArgumentCommandNode<*, *>) {
                        val provider = value.customSuggestions
                        if (provider != null) {
                            if (askServerSuggestion.isAskServerSuggestion(provider)) {
                                logger.info { "Clearing command data for ${entry.key}" }
                                i.remove()
                                nodeAccessor.getArguments(node).remove(entry.key)
                            } else {
                                logger.warn { "Command uses custom suggestions" }
                            }
                        } else {
                            logger.info { "leaf has no suggestions whatsoever" }
                        }
                    }
                }
            }
            node.addChild(augmentedNode)
            processed.add(name)
        }
        if (processed.isEmpty()) {
            logger.warn { "No commands were processed. We'll register them in the vanilla dispatcher anyway" }
            for (name in names) {
                vanillaDispatcher.register(
                    literal<ICommandSource>(name)
                        .then(augmentedNode)
                )
            }
            processed.addAll(names)
        }
        return processed
    }
}

private fun buildCmdArgs(
    argsMap: Map<String, TextProvider>,
    executor: CommandExecutor,
): RequiredArgumentBuilder<ICommandSource, String> {
    assert(argsMap.isNotEmpty())
    val cmd = executor.asCommand()
    fun createArgument(entry: Map.Entry<String, TextProvider>):
            RequiredArgumentBuilder<ICommandSource, String> {
        return argument<ICommandSource, String>(
            entry.key,
            broadString(),
        ).suggests { ctx, builder ->
            if (ctx.getArgumentSafely(entry.key) != null) {
                return@suggests builder.buildFuture()
            }
            entry.value.get().thenApply { suggestion ->
                if (suggestion != null) {
                    builder.suggest(suggestion)
                }
                builder.build()
            }
        }.executes(cmd)
    }
    lateinit var root: RequiredArgumentBuilder<ICommandSource, String>
    for (entry in argsMap.asSequence().take(1)) {
        root = createArgument(entry)
    }
    var b = root
    for (entry in argsMap.asSequence().drop(1)) {
        b = b.then(createArgument(entry))
    }
    b.then(
        argument<ICommandSource, String>(
            "",
            StringArgumentType.greedyString()
        ).executes(cmd)
    )
    return root
}

private fun interface CommandExecutor {
    fun execute(ctx: CommandContext<ICommandSource>)
    fun asCommand() = Command { ctx ->
        execute(ctx)
        0
    }
}