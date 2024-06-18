package net.bitbylogic.logicutils.config;

import net.bitbylogic.apibylogic.config.LogicConfig;
import net.bitbylogic.apibylogic.util.message.config.annotation.ConfigValue;

import java.io.File;

public class Messages extends LogicConfig {

    @ConfigValue(path = "Messages.Test-Message")
    public static String testMessage = "I'd love to eat a %food%!";

    public Messages(File configFile) {
        super(configFile);
    }

}
