package com.turikhay.mc.pwam.mc

import com.turikhay.mc.pwam.common.PasswordPattern
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

fun PasswordPattern.compPatternOf(pwd: String) =
    text(patternOf(pwd))
        .hoverEvent(
            HoverEvent.showText(
                translatable("pwam.pwd-pattern-hover")
            )
        )
        .clickEvent(
            ClickEvent.copyToClipboard(pwd)
        )
        .color(
            // Can you guess the password based on its color?
            seedTextColor(pwd)
        )