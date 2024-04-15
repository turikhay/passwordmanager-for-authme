package com.turikhay.mc.pwam.common.text

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

fun interface TextProvider : Supplier<CompletableFuture<String?>>

data class DelegateTextProvider(
    val name: String,
    val executor: Executor,
    val fn: () -> String?,
): TextProvider {
    override fun get(): CompletableFuture<String?> =
        CompletableFuture.supplyAsync(fn, executor)
}

fun provideText(
    name: String,
    executor: Executor,
    fn: () -> String?,
) = DelegateTextProvider(name, executor, fn)
