package net.bitbylogic.logicutils.player;

import net.bitbylogic.apibylogic.util.message.format.Formatter;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class PlayerUtil {

    public static void sendRichMessages(CommandSender sender, BaseComponent... component) {
        for (BaseComponent c : component) {
            sender.spigot().sendMessage(c);
        }
    }

    public static void attemptToGiveItems(Player player, boolean alert, ItemStack... items) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(items);

        if (droppedItems.isEmpty()) {
            return;
        }

        droppedItems.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));

        if (alert) {
            player.sendMessage(Formatter.error("Items", "Some of your items were dropped on the ground!"));
        }
    }

}
