package com.turikhay.mc.pwam.mc

import com.mojang.brigadier.context.CommandContext
import com.turikhay.mc.pwam.common.text.PasswordPairProvider
import com.turikhay.mc.pwam.common.text.Pairs
import com.turikhay.mc.pwam.common.PasswordChangeCallback
import com.turikhay.mc.pwam.common.PasswordPattern
import com.turikhay.mc.pwam.common.PatternCommandRewriter
import io.github.oshai.kotlinlogging.KotlinLogging
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class KnownCommandHandler(
    private val platformCommandDispatcher: PlatformCommandDispatcher,
    private val patternCommandRewriter: PatternCommandRewriter,
    private val pairs: Pairs,
    private val passwordChangeCallback: PasswordChangeCallback,
    private val notificator: Notificator,
    private val pattern: PasswordPattern,
) {
    fun onLoginCommand(
        ctx: CommandContext<*>,
    ) = onAuthCommand(
        pairs.login,
        ctx,
        forceUpdatesPassword = false,
    )

    fun onRegisterCommand(
        ctx: CommandContext<*>,
    ) {
        val pwArg = ctx.getArgument("password")
        val pwRepeatArg = ctx.getArgumentSafely("password-again")
        if (pwRepeatArg != null && pwArg != pwRepeatArg) {
            return checkArguments()
        }
        return onAuthCommand(
            pairs.register,
            ctx,
            forceUpdatesPassword = true,
        )
    }

    fun onChangePasswordCommand(
        ctx: CommandContext<*>,
    ) {
        val oldPwArg = ctx.getArgument("old-password")
        val newPwArg = ctx.getArgument("new-password")
        val restArg = ctx.getArgumentSafely("") ?: ""
        val currentPasswordF = pairs.login.queryBoth()
        val newPasswordF = pairs.register.queryBoth()
        CompletableFuture.allOf(currentPasswordF, newPasswordF).thenRun {
            val oldPwPair = currentPasswordF.get()
            val newPwPair = newPasswordF.get()
            val cmd = StringBuilder(ctx.getLabel()).append(" ")
            if (oldPwPair != null && oldPwPair.pattern == oldPwArg) {
                cmd.append(oldPwPair.password)
            } else {
                cmd.append(oldPwArg)
            }
            cmd.append(" ")
            val newPassword = if (newPwPair != null && newPwPair.pattern == newPwArg) {
                newPwPair.password
            } else {
                newPwArg
            }
            if (pattern.containsPattern(newPassword)) {
                return@thenRun checkArguments()
            }
            changePassword(newPassword)
            cmd.append(newPassword).append(" ").append(restArg)
            platformCommandDispatcher.dispatchCommand(cmd.toString(), ctx.input)
        }
    }

    fun onUnregisterCommand(
        ctx: CommandContext<*>,
    ) {
        val pwArg = ctx.getArgument("password")
        val rest = ctx.getArgumentSafely("") ?: ""
        pairs.login.queryBoth().thenAccept { pair ->
            val password = if (pair != null && pair.pattern == pwArg) {
                pair.password
            } else {
                pwArg
            }
            if (pattern.containsPattern(password)) {
                return@thenAccept checkArguments()
            }
            platformCommandDispatcher.dispatchCommand(
                "${ctx.getLabel()} $password $rest",
                ctx.input
            )
        }
    }

    private fun onAuthCommand(
        pairProvider: PasswordPairProvider,
        ctx: CommandContext<*>,
        forceUpdatesPassword: Boolean,

    ) {
        val pwArg = ctx.getArgument("password")
        pairProvider.queryBoth().thenAccept { pair ->
            var updatePassword = forceUpdatesPassword
            val newPassword = if (pair != null && pwArg == pair.pattern) {
                pair.password
            } else {
                updatePassword = true
                pwArg
            }
            if (pattern.containsPattern(newPassword)) {
                return@thenAccept checkArguments()
            }
            if (updatePassword) {
                changePassword(newPassword)
            }
            patternCommandRewriter.rewriteLater(ctx.input).thenAccept { cmd ->
                platformCommandDispatcher.dispatchCommand(cmd, ctx.input)
            }.whenComplete { _, t ->
                if (t != null) {
                    logger.error(t) { "Error sending or rewriting the command: ${ctx.input}" }
                    notificator.errorOccurred()
                }
            }
        }
    }

    private fun changePassword(newPassword: String) {
        passwordChangeCallback.changePassword(newPassword)
    }

    private fun checkArguments() {
        return notificator.send {
            Component.translatable("pwam.cmd.input.mistake")
                .color(NamedTextColor.RED)
        }
    }
}
