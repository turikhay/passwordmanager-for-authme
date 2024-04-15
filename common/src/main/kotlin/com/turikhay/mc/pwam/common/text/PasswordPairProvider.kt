package com.turikhay.mc.pwam.common.text

import java.util.concurrent.CompletableFuture

data class PasswordPairProvider (
    val password: TextProvider,
    val pattern: TextProvider,
) {
    fun queryBoth(): CompletableFuture<PasswordPair?> {
        val passwordF = password.get()
        val patternF = pattern.get()
        return CompletableFuture.allOf(passwordF, patternF).thenApply {
            val password = passwordF.get()
            val pattern = patternF.get()
            if (password != null && pattern != null) {
                PasswordPair(password, pattern)
            } else {
                null
            }
        }
    }
}

data class PasswordPair(
    val password: String,
    val pattern: String,
)

data class Pairs(
    val login: PasswordPairProvider,
    val register: PasswordPairProvider,
) {
    fun asList() = listOf(login, register)
}