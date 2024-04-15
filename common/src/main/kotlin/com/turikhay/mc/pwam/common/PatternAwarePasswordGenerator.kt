package com.turikhay.mc.pwam.common

import com.turikhay.mc.pwam.common.text.CacheableTextProvider
import com.turikhay.mc.pwam.common.text.TextProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

data class PatternAwarePasswordGenerator(
    private val delegate: TextProvider,
    private val patternFactory: PasswordPattern,
    private val patternProvider: TextProvider,
    private val maxAttempts: Int,
) : TextProvider {
    override fun get(): CompletableFuture<String?> =
        patternProvider.get().thenCompose { existingPattern ->
            generateUntilUnique(existingPattern)
        }

    private fun generateUntilUnique(existingPattern: String?): CompletableFuture<String?> {
        if (existingPattern == null) {
            return delegate.get()
        }
        val future = CompletableFuture<String?>()
        tryGenerate(future, existingPattern, 1)
        return future
    }

    private fun tryGenerate(
        future: CompletableFuture<String?>,
        existingPattern: String,
        attempt: Int
    ) {
        if (attempt > maxAttempts) {
            future.completeExceptionally(NonUniquePatternException())
            return
        }
        if (delegate is CacheableTextProvider) {
            delegate.invalidate()
        }
        delegate.get().whenComplete { pwd, t ->
            if (t != null) {
                future.completeExceptionally(t)
            } else if (pwd == null) {
                future.complete(null)
            } else {
                val pattern = patternFactory.patternOf(pwd)
                if (pattern == existingPattern) {
                    logger.warn {
                        "Oh wow! We've generated the password, and its pattern is " +
                                "identical to the existing: $existingPattern"
                    }
                    // try again
                    tryGenerate(future, existingPattern, attempt + 1)
                } else {
                    if (attempt > 1) {
                        logger.warn {
                            "We've generated a unique password now, all good: $pattern"
                        }
                    }
                    future.complete(pwd)
                }
            }
        }
    }

    class NonUniquePatternException : Exception(
        "Failed to create a unique password",
        null,
        false,
        false,
    )
}