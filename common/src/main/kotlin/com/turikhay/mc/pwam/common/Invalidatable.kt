package com.turikhay.mc.pwam.common

interface Invalidatable {
    fun invalidate()
}

fun invalidateAll(vararg caches: Invalidatable) = caches.forEach { it.invalidate() }