package com.turikhay.mc.pwam.common

import com.google.common.util.concurrent.MoreExecutors
import com.turikhay.mc.pwam.common.text.provideText
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger

class PatternAwarePasswordGeneratorTest {

    private val executor = MoreExecutors.directExecutor()

    @Test
    fun `fails on non-unique password`() {
        val patternFactory = PasswordPattern(true)
        val f = PatternAwarePasswordGenerator(
            provideText("same", executor) {
                "same"
            },
            patternFactory,
            provideText("pattern of same", executor) {
                patternFactory.patternOf("same")
            },
            1,
        ).get()
        val t = assertThrows(ExecutionException::class.java) {
            f.get()
        }
        assertInstanceOf(
            PatternAwarePasswordGenerator.NonUniquePatternException::class.java,
            t.cause
        )
    }

    @Test
    fun `repeats on non-unique password`() {
        val patternFactory = PasswordPattern(true)
        val cnt = AtomicInteger(4)
        val f = PatternAwarePasswordGenerator(
            provideText("unique", executor) {
                arrayOf(
                    "unique",
                    "non-unique",
                    "non-unique",
                    "non-unique",
                )[cnt.decrementAndGet()]
            },
            patternFactory,
            provideText("non-unique pattern", executor) {
                patternFactory.patternOf("non-unique")
            },
            99,
        ).get()
        assertEquals("unique", f.get())
    }
}