package com.turikhay.mc.pwam.mc

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext

fun CommandContext<*>.getLabel(): String =
    this.input.split(" ", limit = 2)[0]

fun <T, V> CommandContext<T>.getArgumentSafely(name: String, clazz: Class<V>): V? {
    return try {
        getArgument(name, clazz)
    } catch (ignored: IllegalArgumentException) {
        null
    }
}

fun <T> CommandContext<T>.getArgument(name: String): String =
    getArgument(name, String::class.java)

fun <T> CommandContext<T>.getArgumentSafely(name: String): String? =
    getArgumentSafely(name, String::class.java)

class BroadStringArgumentType : ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        val start: Int = reader.cursor
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip()
        }
        return reader.string.substring(start, reader.cursor)
    }

    companion object {
        fun broadString() = BroadStringArgumentType()
    }
}