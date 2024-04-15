package com.turikhay.mc.pwam.fabric.common;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.turikhay.mc.pwam.fabric.mixin.LocalSuggestionProviderAccessor;
import com.turikhay.mc.pwam.mc.IAskServerSuggestion;
import net.minecraft.command.suggestion.SuggestionProviders;

import java.util.Optional;

public class FabricAskServerSuggestion implements IAskServerSuggestion {
    private static final LocalSuggestionProviderAccessor ASK_SERVER =
            getAccessor(SuggestionProviders.ASK_SERVER).orElseThrow();

    public static Optional<LocalSuggestionProviderAccessor> getAccessor(
            SuggestionProvider<?> provider
    ) {
        if (provider instanceof SuggestionProviders.LocalProvider local) {
            return Optional.of((LocalSuggestionProviderAccessor) local);
        }
        return Optional.empty();
    }

    public boolean isAskServerSuggestion(SuggestionProvider<?> provider) {
        if (provider == ASK_SERVER.pwam$getProvider()) {
            // Even though we expect provider to be of
            // SuggestionProviders.LocalProvider type, the compiler will
            // sometimes just inline lambda provider directly
            return true;
        }
        return getAccessor(provider)
                .map(acc -> acc.pwam$getId().equals(ASK_SERVER.pwam$getId()))
                .orElse(false);
    }
}
