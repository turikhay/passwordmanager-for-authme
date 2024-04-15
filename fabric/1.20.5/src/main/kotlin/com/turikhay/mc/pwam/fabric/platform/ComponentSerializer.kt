package com.turikhay.mc.pwam.fabric.platform

import com.google.gson.JsonElement
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

fun deserializeComponent(json: JsonElement) =
    Text.Serialization.fromJsonTree(
        json,
        MinecraftClient.getInstance().player!!.registryManager,
    )