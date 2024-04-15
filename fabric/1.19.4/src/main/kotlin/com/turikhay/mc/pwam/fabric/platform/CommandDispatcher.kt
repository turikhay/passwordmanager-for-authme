package com.turikhay.mc.pwam.fabric.platform

import net.minecraft.client.MinecraftClient

fun dispatchCommand(client: MinecraftClient, command: String) {
    client.run {
        networkHandler!!.sendCommand(command)
    }
}

fun addCommandToHistory(client: MinecraftClient, command: String) {
    client.run {
        inGameHud.chatHud.addToMessageHistory("/$command")
    }
}
