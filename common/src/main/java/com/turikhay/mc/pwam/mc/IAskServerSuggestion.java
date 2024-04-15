package com.turikhay.mc.pwam.mc;

import com.mojang.brigadier.suggestion.SuggestionProvider;

public interface IAskServerSuggestion {
    boolean isAskServerSuggestion(SuggestionProvider<?> provider);
}
