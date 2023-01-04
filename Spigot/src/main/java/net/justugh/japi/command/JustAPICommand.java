package net.justugh.japi.command;

import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.util.Format;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class JustAPICommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("justapi.admin")) {
            sender.sendMessage(Format.format("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Format.format("&cJustAPI Commands&8:"));
            sender.sendMessage(Format.format("&8 - &9/justapi debug &8- &7Toggle debug logging. (WARNING: CAN BE SPAMMY)"));
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            JustAPIPlugin.getInstance().toggleDebug();
            sender.sendMessage(Format.format("&cJustAPI &8- &9Debug has been " + (JustAPIPlugin.getInstance().isDebug() ? "&aEnabled" : "&cDisabled")));
        }

        return true;
    }

}
