package com.turikhay.mc.pwam.fabric

import com.turikhay.mc.pwam.common.PasswordPattern
import com.turikhay.mc.pwam.common.initDbLater
import com.turikhay.mc.pwam.fabric.common.FabricAskServerSuggestion
import com.turikhay.mc.pwam.fabric.common.FabricCommandNodeAccessor
import com.turikhay.mc.pwam.fabric.platform.SUPPORTS_EMOJI
import com.turikhay.mc.pwam.fabric.platform.deserializeComponent
import com.turikhay.mc.pwam.mc.IClient
import com.turikhay.mc.pwam.mc.Session
import com.turikhay.mc.pwam.mc.SessionInfo
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.json.JSONOptions
import net.kyori.adventure.translation.GlobalTranslator
import net.minecraft.SharedConstants
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Util
import org.jetbrains.exposed.sql.Database
import java.util.*
import java.util.concurrent.CompletableFuture

class PWAM : ModInitializer {
    private lateinit var dbFuture: CompletableFuture<Database>

    override fun onInitialize() {
        INSTANCE = this
        dbFuture = initDbLater(worker)
        initEvents()
    }

    private fun initEvents() {
        ClientPlayConnectionEvents.JOIN.register { handler, _, client ->
            Session.session = Session(
                Client,
                SessionInfo(
                    client.player!!.gameProfile.name,
                    client.currentServerEntry?.address ?: handler.connection.run {
                        if (isLocal) "local" else address.toString()
                    },
                ),
                dbFuture,
                worker,
                FabricCommandNodeAccessor(),
                FabricAskServerSuggestion(),
                PasswordPattern(SUPPORTS_EMOJI),
            )
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            Session.session?.cleanUp()
            Session.session = null
        }
    }

    private val worker by lazy {
        Util.getMainWorkerExecutor()
    }

    companion object {
        lateinit var INSTANCE: PWAM
    }
}

private object Client : IClient {
    override fun dispatchCommand(command: String) =
        com.turikhay.mc.pwam.fabric.platform.dispatchCommand(
            client(),
            command,
        )

    override fun addCommandToHistory(command: String) =
        com.turikhay.mc.pwam.fabric.platform.addCommandToHistory(
            client(),
            command,
        )

    val gsonSerializer: GsonComponentSerializer by lazy {
        GsonComponentSerializer.builder()
            .options(
                JSONOptions.byDataVersion()
                    .at(SharedConstants.getGameVersion().saveVersion.id)
            )
            .build()
    }

    override fun sendMessage(text: Component) {
        client().inGameHud.chatHud.addMessage(
            deserializeComponent(
                gsonSerializer.serializeToTree(
                    GlobalTranslator.render(text, Locale.getDefault())
                )
            )
        )
    }

    override fun getVersion(): String =
        FabricLoader.getInstance().getModContainer("pwam").get().metadata.version.friendlyString

    private fun client(): MinecraftClient = MinecraftClient.getInstance()!!
}