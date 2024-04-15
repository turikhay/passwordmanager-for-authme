package com.turikhay.mc.pwam.common

import com.turikhay.mc.pwam.common.text.PasswordPair
import com.turikhay.mc.pwam.common.text.PasswordPairProvider
import com.turikhay.mc.pwam.mc.Notificator
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class PatternCommandRewriter(
    private val providers: List<PasswordPairProvider>,
    private val notificator: Notificator,
    private val patternDetector: PasswordPatternDetector,
) {
    fun rewriteNow(cmd: String): String {
        if (!patternDetector.containsPattern(cmd)) {
            logger.info { "Command doesn't contain a pattern" }
            return cmd
        }
        logger.info { "Command contains a pattern" }
        var dispatch: String
        try {
            dispatch = doRewrite(
                cmd,
                providersMapSeq().filter {
                    val done = it.second.isDone
                    if (!done) {
                        logger.warn { "Pair is not yet resolved: ${it.first}" }
                    }
                    done
                }.map {
                    it.second.get()
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Error processing the command: $cmd" }
            dispatch = cmd
        }
        logger.info { "Will dispatch: $dispatch" }
        return dispatch
    }

    fun rewriteLater(cmd: String): CompletableFuture<String> {
        val futures = providersMapSeq().map { it.second }.toList().toTypedArray()
        return CompletableFuture.allOf(*futures).thenApply {
            doRewrite(
                cmd,
                futures.asSequence().map { it.get() })
        }
    }

    private fun doRewrite(cmd: String, pairs: Sequence<PasswordPair?>): String {
        var r = cmd
        for (pair in pairs.filterNotNull()) {
            r = r.replace(pair.pattern, pair.password)
        }
        if (r != cmd) {
            notificator.passwordUseNotification()
        }
        return r
    }

    private fun providersMapSeq() = providers.asSequence().map { provider ->
        provider to provider.queryBoth()
    }
}