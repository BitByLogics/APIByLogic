package net.justugh.japi.module.command;

import org.bukkit.command.CommandSender;

public interface ModuleCommandExecutor {

    void execute(CommandSender sender, String[] args);

}
