package com.turikhay.mc.pwam.fabric.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.turikhay.mc.pwam.mc.ICommandSource;
import com.turikhay.mc.pwam.mc.Session;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class CommandTreeMixin {
    @Shadow
    private CommandDispatcher<ICommandSource> commandDispatcher;

    @Inject(method = "onCommandTree", at = @At("RETURN"))
    private void modifyCommandTree(CommandTreeS2CPacket packet, CallbackInfo info) {
        var session = Session.Companion.getSession();
        if (session != null) {
            session.getCommandDispatcherHandler().onCommandTreeReceived(this.commandDispatcher);
        }
    }
}
