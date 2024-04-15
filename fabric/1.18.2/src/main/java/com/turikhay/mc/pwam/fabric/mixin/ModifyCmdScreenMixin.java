package com.turikhay.mc.pwam.fabric.mixin;

import com.turikhay.mc.pwam.fabric.common.ModifyCmd;
import com.turikhay.mc.pwam.mc.ICommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Screen.class)
public class ModifyCmdScreenMixin {
    @Unique
    private String maskedCommand;

    @ModifyVariable(
            method = "sendMessage(Ljava/lang/String;Z)V",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private String modifyCommand(String line) {
        this.maskedCommand = null;
        String newCommand = ModifyCmd.modifyCmd(
                StringUtils.trim(line),
                (ICommandSource) MinecraftClient.getInstance()
                        .getNetworkHandler()
                        .getCommandSource()
        );
        if (newCommand.isEmpty()) {
            return "";
        } else {
            this.maskedCommand = line;
            return "/" + newCommand;
        }
    }

    @ModifyArg(
            method = "sendMessage(Ljava/lang/String;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud;addToMessageHistory(Ljava/lang/String;)V"
            ),
            index = 0
    )
    private String maskPasswordCommand(String message) {
        if (this.maskedCommand != null) {
            var masked = this.maskedCommand;
            this.maskedCommand = null;
            return masked;
        }
        return message;
    }
}
