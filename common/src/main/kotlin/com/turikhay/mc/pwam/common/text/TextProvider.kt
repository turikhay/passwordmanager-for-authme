package com.turikhay.mc.pwam.common.text

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

fun interface TextProvider : Supplier<CompletableFuture<String?>>

data class DelegateTextProvider(
    val name: String,
    val fn: () -> CompletableFuture<String?>,
): TextProvider {
    override fun get(): CompletableFuture<String?> = fn()
}

fun provideText(
    name: String,
    fn: () -> CompletableFuture<String?>,
) = DelegateTextProvider(name, fn)

fun provideText(
    name: String,
    executor: Executor,
    fn: () -> String?,
) = DelegateTextProvider(name) {
    CompletableFuture.supplyAsync(fn, executor)
}
