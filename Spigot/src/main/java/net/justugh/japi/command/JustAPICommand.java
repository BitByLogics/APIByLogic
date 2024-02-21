package net.justugh.japi.command;

import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuItem;
import net.justugh.japi.menu.Rows;
import net.justugh.japi.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JustAPICommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("justapi.admin")) {
            sender.sendMessage(Format.format("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Format.format("&cJustAPI Commands&8:"));
            sender.sendMessage(Format.format("&8 - &9/justapi debug &8- &7Toggle debug logging"));
            sender.sendMessage(Format.format("&8 - &9/justapi redislisteners &8- &7View registered redis listeners"));
            sender.sendMessage(Format.format("&8 - &9/justapi menus &8- &7View registered menus"));
            sender.sendMessage(Format.format("&8 - &9/justapi boards &8- &7View registered scoreboards"));
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
