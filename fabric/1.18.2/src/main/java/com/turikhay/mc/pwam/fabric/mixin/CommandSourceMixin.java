package com.turikhay.mc.pwam.fabric.mixin;

import com.turikhay.mc.pwam.mc.ICommandSource;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CommandSource.class)
public interface CommandSourceMixin extends ICommandSource {
}
