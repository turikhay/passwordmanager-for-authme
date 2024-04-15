package com.turikhay.mc.pwam.common

import com.turikhay.mc.pwam.common.text.PasswordPairProvider
import com.turikhay.mc.pwam.mc.Notificator
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class PatternCommandRewriter(
    private val providers: List<PasswordPairProvider>,
    private val notificator: Notificator
) {
    fun rewriteCommand(cmd: String): String {
        var dispatch = cmd
        try {
            val pairs = providers.asSequence().map { provider ->
                provider to provider.queryBoth()
            }.filter {
                val done = it.second.isDone
                if (!done) {
                    logger.warn { "Pair is not resolved: ${it.first}" }
                }
                done
            }.map {
                it.second.get()
            }
            var r = cmd
            for (pair in pairs) {
                if (pair != null) {
                    r = r.replace(pair.pattern, pair.password)
                }
            }
            dispatch = r
            if (dispatch != cmd) {
                notificator.passwordUseNotification()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error processing the command: $cmd" }
        }
        logger.info { "Will dispatch: $dispatch" }
        return dispatch
    }
}