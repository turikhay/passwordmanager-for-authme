package com.turikhay.mc.pwam.fabric.mixin;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = CommandNode.class, remap = false)
public interface CommandNodeAccessor<S> {
    @Accessor("children")
    Map<String, CommandNode<S>> getChildren();

    @Accessor("arguments")
    Map<String, ArgumentCommandNode<S, ?>> getArguments();
}
