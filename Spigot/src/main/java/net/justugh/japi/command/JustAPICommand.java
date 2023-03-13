package net.justugh.japi.command;

import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.util.Format;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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

        if (args[0].equalsIgnoreCase("redislisteners")) {
            int page = 1;

            if (args.length >= 2) {
                page = Integer.parseInt(args[1]);
            }

            List<String> data = new ArrayList<>();
            JustAPIPlugin.getInstance().getRedisManager().getClients().forEach(client -> {
                client.getListeners().forEach(listener -> {
                    data.add(listener.getChannelName() + " &8(&7Self Activating&8: " + (listener.isSelfActivation() ? "&aTrue" : "&cFalse") + "&8)");
                });
            });

            sender.sendMessage(Format.getPagedList("&cRegistered Redis Listeners", data, page));
            return true;
        }

        if (args[0].equalsIgnoreCase("menus")) {
            int page = 1;

            if (args.length >= 2) {
                page = Integer.parseInt(args[1]);
            }

            List<String> data = new ArrayList<>();
            JustAPIPlugin.getInstance().getMenuManager().getActiveMenus().forEach(menu -> {
                data.add((menu.getId() == null ? menu.getTitle() : menu.getId()) + " &8(" + (menu.getUpdateTask() == null ? "&cNot Updating" : "&aUpdating") + "&8)");
            });

            sender.sendMessage(Format.getPagedList("&cMenu Data", data, page));
            return true;
        }

        if (args[0].equalsIgnoreCase("boards")) {
            int page = 1;

            if (args.length >= 2) {
                page = Integer.parseInt(args[1]);
            }

            List<String> data = new ArrayList<>();
            JustAPIPlugin.getInstance().getActiveBoards().forEach(board -> {
                data.add(board.getId() + " &8(" + (board.getUpdateTask() == null ? "&cNot Updating" : "&aUpdating") + "&8)");
            });

            sender.sendMessage(Format.getPagedList("&cBoard Data", data, page));
            return true;
        }

        return true;
    }

}
