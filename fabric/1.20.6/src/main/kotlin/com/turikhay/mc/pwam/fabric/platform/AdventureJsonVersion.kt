package com.turikhay.mc.pwam.fabric.platform

import net.minecraft.SharedConstants

fun adventureJsonVersion(): Int {
    if (SharedConstants.getProtocolVersion() < 769) { // < 1.21.4
        return 3819
    }
    return 4174
}