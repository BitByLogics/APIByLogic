package net.bitbylogic.logicutils.menu.command;

import net.bitbylogic.apibylogic.acf.BaseCommand;
import net.bitbylogic.apibylogic.acf.annotation.*;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import net.bitbylogic.logicutils.LogicUtils;
import net.bitbylogic.logicutils.menu.RuntimeMenusManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("runtimemenus")
@CommandPermission("logicutils.command.runtimemenus")
public class RuntimeMenusCommand extends BaseCommand {

    @Dependency
    private LogicUtils plugin;

    @Dependency
    private RuntimeMenusManager runtimeMenusManager;

    @Default
    public void onDefault(CommandSender sender) {
        sender.sendMessage(Formatter.listHeader("Runtime Menus", ""));
        sender.sendMessage(Formatter.command("rmenus reload", "Reload all menus from configuration."));
        sender.sendMessage(Formatter.command("rmenus list <page>", "List all menus."));
        sender.sendMessage(Formatter.command("rmenus open <menu id> (player)", "Open a Menu for yourself or others."));
    }

    @Subcommand("reload")
    public void onReload(CommandSender sender) {
        runtimeMenusManager.loadMenus();
        sender.sendMessage(Formatter.success("Runtime Menus", "Reloaded menus from configuration!"));
    }

    @Subcommand("list")
    public void onList(CommandSender sender, @Default("1") int page) {
        displayPage(sender, page);
    }

    @Subcommand("open")
    public void onOpen(Player sender, String menuId, @Optional Player target) {
        Menu menu = runtimeMenusManager.getLoadedMenus().get(menuId);

        if (menu == null) {
            sender.sendMessage(Formatter.error("Runtime Menus", "Invalid menu."));
            return;
        }

        if (target != null) {
            target.openInventory(menu.getInventory());
            return;
        }

        ((Player) sender).openInventory(menu.getInventory());
    }

    private void displayPage(CommandSender sender, int page) {
        List<Menu> menus = new ArrayList<>(runtimeMenusManager.getLoadedMenus().values());
        int pages = menus.size() / 10.0d % 1 == 0 ? menus.size() / 10 : menus.size() / 10 + 1;
        int lastPossibleMenu = menus.size();

        if (page != 0 && page <= pages) {
            int startingMenu = (page * 10) - 10;
            int lastMenu = Math.min(startingMenu + 10, lastPossibleMenu);
            sender.sendMessage(Formatter.color("&8&m-----(&r &eMenu List &8&m)-----"));
            for (int i = startingMenu; i < lastMenu; i++) {
                Menu menu = menus.get(i);
                if (sender instanceof Player) {
                    TextComponent message = new TextComponent(String.format(Formatter.color("&8- &e%s"), menu.getId()));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Formatter.color("&eClick to open!"))));
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rmenus open " + menu.getId()));
                    ((Player) sender).spigot().sendMessage(message);
                } else {
                    sender.sendMessage(String.format("&8- &e%s", menu.getId()));
                }
            }
            sender.sendMessage(String.format(Formatter.color("&8&m--------(&r&ePage&8: &a%s&8&m)--------"), page));
        } else {
            sender.sendMessage(Formatter.error("Runtime Menus", "Invalid page&8: &a" + page));
        }
    }

}
