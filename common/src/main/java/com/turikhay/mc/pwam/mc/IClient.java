package com.turikhay.mc.pwam.mc;

public interface IClient
        extends PlatformCommandDispatcher, PlatformAudience {
    String getVersion();
}
