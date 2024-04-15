package com.turikhay.mc.pwam.common.text

import com.turikhay.mc.pwam.common.Invalidatable
import java.util.concurrent.atomic.AtomicInteger

class TextProvidersCache(
    private val providers: List<TextProvider>
) : Invalidatable {
    private val updateId = AtomicInteger()
    val values = Array<String?>(providers.size) {
        null
    }

    init {
        // TODO update later?
        invalidate()
    }

    fun includes(text: String): Boolean {
        for (value in values) {
            if (value != null && text.contains(value)) {
                return true
            }
        }
        return false
    }

    override fun invalidate() {
        val expectedUpdateId = updateId.incrementAndGet()
        this.providers.forEachIndexed { index, textProvider ->
            textProvider.get().thenAccept { value ->
                if (updateId.get() == expectedUpdateId) {
                    values[index] = value
                }
            }
        }
    }
}