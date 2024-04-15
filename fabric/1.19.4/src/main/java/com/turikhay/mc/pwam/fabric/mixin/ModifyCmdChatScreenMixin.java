package com.turikhay.mc.pwam.fabric.mixin;

import com.turikhay.mc.pwam.fabric.common.ModifyCmd;
import com.turikhay.mc.pwam.mc.ICommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatScreen.class)
public abstract class ModifyCmdChatScreenMixin {
    @Shadow
    public abstract String normalize(String chatText);

    @Unique
    private String maskedCommand;

    @ModifyVariable(
            method = "sendMessage",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private String modifyCommand(String line) {
        // reset memoized og cmd
        this.maskedCommand = null;
        String newCommand = ModifyCmd.modifyCmd(
                this.normalize(line),
                (ICommandSource) MinecraftClient.getInstance()
                        .getNetworkHandler()
                        .getCommandSource()
        );
        if (newCommand.isEmpty()) {
            // empty cmd will be cancelled down the line
            return "";
        } else {
            // memoize og cmd to add it in the history
            this.maskedCommand = line;
            // use new command
            return "/" + newCommand;
        }
    }

    @ModifyArg(
            method = "sendMessage",
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
