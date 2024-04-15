package com.turikhay.mc.pwam.common

import java.security.SecureRandom

private val random = SecureRandom()
private val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray()

fun generatePassword(len: Int): String {
    val sb = StringBuilder(len)
    for (i in 0 until len) {
        val randomIndex = random.nextInt(chars.size)
        sb.append(chars[randomIndex])
    }
    return sb.toString()
}