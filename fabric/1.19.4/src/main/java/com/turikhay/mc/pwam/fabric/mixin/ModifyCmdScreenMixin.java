package com.turikhay.mc.pwam.fabric.mixin;

import com.turikhay.mc.pwam.fabric.common.ModifyCmd;
import com.turikhay.mc.pwam.mc.ICommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Screen.class)
public class ModifyCmdScreenMixin {
    @Redirect(
            method = "handleTextClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendCommand(Ljava/lang/String;)Z"
            )
    )
    private boolean modifyTextClickCommand(ClientPlayNetworkHandler instance, String line) {
        String newCommand = ModifyCmd.modifyCmd(
                line,
                (ICommandSource) MinecraftClient.getInstance()
                        .getNetworkHandler()
                        .getCommandSource(),
                false
        );
        if (!newCommand.isEmpty()) {
            return instance.sendCommand(newCommand);
        }
        // command was dispatched by us, no need to call the vanilla dispatcher
        return true;
    }
}
