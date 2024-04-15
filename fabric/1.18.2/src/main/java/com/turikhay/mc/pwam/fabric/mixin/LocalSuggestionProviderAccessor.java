package com.turikhay.mc.pwam.fabric.mixin;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SuggestionProviders.LocalProvider.class)
public interface LocalSuggestionProviderAccessor {
    @Accessor("name")
    Identifier pwam$getId();

    @Accessor("provider")
    SuggestionProvider<CommandSource> pwam$getProvider();
}

