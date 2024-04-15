package com.turikhay.mc.pwam.mc;

public interface PlatformCommandDispatcher {
    void dispatchCommand(String command);
    void addCommandToHistory(String command);

    default void dispatchCommand(String command, String historyCommand) {
        dispatchCommand(command);
        addCommandToHistory(historyCommand);
    }

    default void dispatchCommandAndAddToHistory(String command) {
        dispatchCommand(command, command);
    }
}
