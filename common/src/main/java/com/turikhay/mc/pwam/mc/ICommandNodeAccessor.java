package com.turikhay.mc.pwam.mc;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Map;

public interface ICommandNodeAccessor<S> {
    Map<String, CommandNode<S>> getChildren(CommandNode<S> node);
    Map<String, ArgumentCommandNode<S, ?>> getArguments(CommandNode<S> node);
}
