package com.turikhay.mc.pwam.common

import java.util.concurrent.CompletableFuture

fun <T> CompletableFuture<T>.getNowOrFail(): T {
    if (isDone) {
        return get()
    } else {
        throw IllegalStateException("Is not done yet: $this")
    }
}