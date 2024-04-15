package com.turikhay.mc.pwam.common

interface PasswordPatternDetector {
    fun containsPattern(str: String): Boolean
}