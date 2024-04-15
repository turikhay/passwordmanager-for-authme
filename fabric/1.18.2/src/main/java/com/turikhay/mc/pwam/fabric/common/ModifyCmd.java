package com.turikhay.mc.pwam.fabric.common;

import com.turikhay.mc.pwam.mc.ICommandSource;
import com.turikhay.mc.pwam.mc.Session;

public interface ModifyCmd {
    static String modifyCmd(String line, ICommandSource cmdSource, boolean expectSlash) {
        String command;
        if (expectSlash) {
            if (!line.startsWith("/")) {
                return line;
            }
            command = line.substring(1);
        } else {
            command = line;
        }
        var session = Session.Companion.getSession();
        if (session == null) {
            return line;
        }
        if (session.getCommandDispatcherHandler().isKnownCommand(command)) {
            session.getCommandDispatcherHandler().dispatchKnownCommand(
                    command,
                    cmdSource
            );
            return "";
        }
        return session.getCommandRewriter().rewriteCommand(command);
    }

    static String modifyCmd(String line, ICommandSource cmdSource) {
        return modifyCmd(line, cmdSource, true);
    }
}
