package com.turikhay.mc.pwam.fabric.common;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.turikhay.mc.pwam.fabric.mixin.CommandNodeAccessor;
import com.turikhay.mc.pwam.mc.ICommandNodeAccessor;

import java.util.Map;

@SuppressWarnings("unchecked")
public class FabricCommandNodeAccessor<S> implements ICommandNodeAccessor<S> {
    @Override
    public Map<String, CommandNode<S>> getChildren(CommandNode<S> node) {
        return ((CommandNodeAccessor<S>) node).getChildren();
    }

    @Override
    public Map<String, ArgumentCommandNode<S, ?>> getArguments(CommandNode<S> node) {
        return ((CommandNodeAccessor<S>) node).getArguments();
    }
}
