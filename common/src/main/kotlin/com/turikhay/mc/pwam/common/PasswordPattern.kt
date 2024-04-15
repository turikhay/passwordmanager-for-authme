package com.turikhay.mc.pwam.common

import com.turikhay.mc.pwam.common.text.TextProvider

private val LOCKED = "\uD83D\uDD12" // https://emojipedia.org/locked

data class PasswordPattern(
    private val supportsLockEmoji: Boolean
) {
    fun patternOf(pwd: String): String {
        if (pwd.length < 3) {
            return pwd
        }
        val b = StringBuilder()
        pwd.asSequence().take(2).forEach { c -> b.append(c) }
        for (i in 1 until 4) { b.append("*") }
        if (supportsLockEmoji) {
            b.append(LOCKED)
        }
        return b.toString()
    }

    fun containsPattern(str: String): Boolean {
        return if (supportsLockEmoji) {
            str.contains("*$LOCKED")
        } else {
            str.contains("*".repeat(3))
        }
    }

    fun providePwdPattern(provider: TextProvider) = TextProvider {
        provider.get().thenApply { pwd ->
            if (pwd != null) patternOf(pwd) else null
        }
    }
}