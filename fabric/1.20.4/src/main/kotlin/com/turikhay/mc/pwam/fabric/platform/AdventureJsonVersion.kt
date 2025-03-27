package com.turikhay.mc.pwam.fabric.platform

import net.minecraft.SharedConstants

fun adventureJsonVersion(): Int {
    if (SharedConstants.getProtocolVersion() < 765) { // < 1.20.3
        return 2526
    }
    return 3679
}