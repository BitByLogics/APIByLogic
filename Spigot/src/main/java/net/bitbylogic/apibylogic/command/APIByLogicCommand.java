package net.bitbylogic.apibylogic.command;

import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.util.Format;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class APIByLogicCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("apibylogic.admin")) {
            sender.sendMessage(Format.format("&cNo permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Format.format("&cAPIByLogic Commands&8:"));
            sender.sendMessage(Format.format("&8 - &9/abl debug &8- &7Toggle debug logging"));
            sender.sendMessage(Format.format("&8 - &9/abl redislisteners &8- &7View registered redis listeners"));
            sender.sendMessage(Format.format("&8 - &9/abl menus &8- &7View registered menus"));
            sender.sendMessage(Format.format("&8 - &9/abl boards &8- &7View registered scoreboards"));
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            APIByLogic.getInstance().toggleDebug();
            sender.sendMessage(Format.format("&cAPIByLogic &8- &9Debug has been " + (APIByLogic.getInstance().isDebug() ? "&aEnabled" : "&cDisabled")));
        }

        if (args[0].equalsIgnoreCase("redislisteners")) {
            int page = 1;

            if (args.length >= 2) {
                page = Integer.parseInt(args[1]);
            }

            List<String> data = new ArrayList<>();
            APIByLogic.getInstance().getRedisManager().getClients().forEach(client -> {
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
            APIByLogic.getInstance().getActiveBoards().forEach(board -> {
                data.add(board.getId() + " &8(" + (board.getUpdateTask() == null ? "&cNot Updating" : "&aUpdating") + "&8)");
            });

            sender.sendMessage(Format.getPagedList("&cBoard Data", data, page));
            return true;
        }

        return true;
    }

}
