package com.turikhay.mc.pwam.common.text

import com.turikhay.mc.pwam.common.Invalidatable
import java.util.concurrent.CompletableFuture

data class CacheableTextProvider(
    private val delegate: TextProvider
) : TextProvider, Invalidatable {
    private var cached: CompletableFuture<String?>? = null

    override fun get(): CompletableFuture<String?> {
        if (cached == null) {
            cached = delegate.get()
        }
        return cached!!
    }

    override fun invalidate() {
        cached = null
    }
}