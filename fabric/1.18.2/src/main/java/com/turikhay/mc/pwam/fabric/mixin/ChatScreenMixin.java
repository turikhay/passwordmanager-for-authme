package com.turikhay.mc.pwam.fabric.mixin;

import com.turikhay.mc.pwam.fabric.common.IChatTextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow protected TextFieldWidget chatField;

    @Inject(method = "init", at = @At("TAIL"))
    private void setChatTextFieldWidget(CallbackInfo ci) {
        ((IChatTextFieldWidget) this.chatField).pwam$setChatTextField(true);
    }
}
