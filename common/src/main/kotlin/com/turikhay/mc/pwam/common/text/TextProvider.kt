package com.turikhay.mc.pwam.common.text

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

fun interface TextProvider : Supplier<CompletableFuture<String?>>

fun provideText(
    executor: Executor,
    fn: () -> String?,
) = TextProvider {
    CompletableFuture.supplyAsync(fn, executor)
}