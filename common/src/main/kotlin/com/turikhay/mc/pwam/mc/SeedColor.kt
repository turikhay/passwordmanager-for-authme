package com.turikhay.mc.pwam.mc

import net.kyori.adventure.text.format.TextColor
import java.awt.Color

fun seedColorOf(seed: Int) = Color.HSBtoRGB(
    // 1) take all the bytes
    // 2) remove the exponent
    // 3) then add it back (2^0)
    // 4) the resulting float would be within [1, 2)
    java.lang.Float.intBitsToFloat(seed and 0x007fffff or 0x3f800000),
    0.35f,
    1.0f,
)

fun seedColorOf(any: Any) =
    // Object hash codes (specially of type String) can be pretty... boring
    seedColorOf(any.hashCode().let { it * it })

fun seedTextColor(any: Any) = TextColor.color(seedColorOf(any))