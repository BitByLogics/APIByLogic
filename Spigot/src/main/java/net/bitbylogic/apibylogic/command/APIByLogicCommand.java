package net.bitbylogic.apibylogic.command;

import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.util.Format;
import net.bitbylogic.apibylogic.util.message.LogicColor;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
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
            sender.sendMessage(Formatter.error("APIByLogic", "No permission."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(
                    Formatter.listHeader("APIByLogic", "Commands"),
                    Formatter.command("abl reload", "Reload the configuration."),
                    Formatter.command("abl debug", "Toggle debug logging."),
                    Formatter.command("abl redislisteners", "View registered redis listeners."),
                    Formatter.command("abl boards", "View registered scoreboards.")
            );
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            APIByLogic.getInstance().reloadConfig();
            LogicColor.loadColors(APIByLogic.getInstance().getConfig());
            Formatter.getConfig().loadConfigPaths();
            sender.sendMessage(Formatter.success("APIByLogic", "Successfully reloaded configuration."));
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            APIByLogic.getInstance().toggleDebugMode();
            sender.sendMessage(Formatter.success("APIByLogic", "&cAPIByLogic &8- &9Debug has been %s.", APIByLogic.getInstance().isDebugMode() ? "&aEnabled" : "&cDisabled"));
            return true;
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

            sender.sendMessage(Formatter.getPagedList("Registered Redis Listeners", data, page));
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

            sender.sendMessage(Formatter.getPagedList("&cBoard Data", data, page));
            return true;
        }

        return true;
    }

}
