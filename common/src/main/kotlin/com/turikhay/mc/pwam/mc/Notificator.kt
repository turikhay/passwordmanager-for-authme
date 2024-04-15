package com.turikhay.mc.pwam.mc

import com.turikhay.mc.pwam.common.PasswordPattern
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

class Notificator(
    private val audienceProvider: PlatformAudience,
    private val pattern: PasswordPattern,
) {
    fun passwordUseNotification() = send {
        Component.translatable("pwam.cmd.input.notification")
    }

    fun passwordChangeNotification(newPassword: String) = send {
        Component.translatable("pwam.cmd.input.updated")
            .args(
                Component.text(pattern.patternOf(newPassword))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.translatable("pwam.cmd.input.updated.hover")
                        )
                    ).clickEvent(
                        ClickEvent.copyToClipboard(newPassword)
                    )
            )
    }

    fun send(comp: () -> Component) {
        audienceProvider.sendMessage(
            Component.empty()
                .append(Component.translatable("pwam.prefix").color(NamedTextColor.GOLD))
                .append(Component.space())
                .append(comp())
        )
    }
}