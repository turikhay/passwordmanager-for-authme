package com.turikhay.mc.pwam.fabric.mixin;

import com.turikhay.mc.pwam.fabric.common.IChatTextFieldWidget;
import com.turikhay.mc.pwam.mc.Session;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin implements IChatTextFieldWidget {
    @Shadow private String text;
    @Shadow private int selectionStart;
    @Unique private boolean pwam$isChatTextField = false;

    @Override
    public void pwam$setChatTextField(boolean b) {
        this.pwam$isChatTextField = b;
    }

    @Inject(method = "getCursorPosWithOffset", at = @At("RETURN"), cancellable = true)
    public void skipPasswordPattern(int offset, CallbackInfoReturnable<Integer> cir) {
        if (!pwam$isChatTextField) {
            return;
        }
        int newCursor = cir.getReturnValue();
        if (newCursor >= this.text.length()) {
            return;
        }
        var session = Session.Companion.getSession();
        if (session == null) {
            return;
        }
        var cache = session.getPwdPatternCache();
        if (!cache.includes(this.text)) {
            return;
        }
        for (String password : cache.getValues()) {
            if (password == null) {
                continue;
            }
            var startIdx = StringUtils.indexOf(
                    this.text,
                    password,
                    Math.max(0, this.selectionStart - password.length())
            );
            if (startIdx < 0) {
                continue;
            }
            var endIdx = startIdx + password.length();
            if (newCursor < startIdx || newCursor >= endIdx) {
                continue;
            }
            if (offset > 0) {
                newCursor = endIdx;
            } else {
                newCursor = startIdx;
            }
            cir.setReturnValue(newCursor);
            return;
        }
    }
}
