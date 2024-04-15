package com.turikhay.mc.pwam.mc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;

public interface PlatformAudience {
    void sendMessage(Component text);
    default void sendMessage(ComponentBuilder<?, ?> b) {
        sendMessage(b.build());
    }
}
