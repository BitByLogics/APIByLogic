package net.justugh.japi.module.command;

import org.bukkit.command.CommandSender;

public interface CommandInterface {
	
	void execute(CommandSender sender, String[] args);

}
