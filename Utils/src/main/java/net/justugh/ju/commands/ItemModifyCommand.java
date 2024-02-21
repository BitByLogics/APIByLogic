package net.justugh.ju.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import net.justugh.japi.util.RichTextUtil;
import net.justugh.ju.JustUtils;
import net.justugh.ju.message.Messages;
import net.justugh.ju.player.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("itemmodify|im")
@CommandPermission("justutils.command.itemmodify")
public class ItemModifyCommand extends BaseCommand {

    @Default
    public void onDefault(CommandSender sender) {
        PlayerUtil.sendRichMessages(sender,
                Messages.richFormat(Messages.listHeader("Item Modify", "Valid Commands")),
                Messages.richCommand("itemmodify name <name>", "Change the items name."),
                Messages.richCommand("itemmodify lore <lore>", "Change the items lore."),
                Messages.richCommand("itemmodify model <id>", "Change custom model data id."),
                Messages.richCommand("itemmodify persistent <key> <value>", "Apply a persistent data value.")
        );
    }

    @Subcommand("model")
    public void onModelCommand(Player player, int id) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(id);
        item.setItemMeta(meta);
        player.sendMessage(Messages.success("Item Modify", "Successfully set model id to %s!", id));
    }

    @Subcommand("name")
    public void onNameCommand(Player player, String name) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Messages.format(name));
        item.setItemMeta(meta);
        player.sendMessage(Messages.success("Item Modify", "Successfully set item name to %s!", name));
    }

    @Subcommand("lore")
    public void onLoreCommand(Player player, String lore) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold an item."));
            return;
        }

        List<String> actualLore = new ArrayList<>();

        for (String loreLine : RichTextUtil.getRichText(lore, 0)) {
            actualLore.add(Messages.format(loreLine));
        }

        ItemMeta meta = item.getItemMeta();
        meta.setLore(actualLore);
        item.setItemMeta(meta);

        player.sendMessage(Messages.richFormat(Messages.success("Item Modify", "Successfully set item lore to: ")));
        AtomicInteger position = new AtomicInteger();
        actualLore.forEach(loreLine -> {
            player.sendMessage(Messages.richFormat(Messages.listItem("#" + position.incrementAndGet(), loreLine)));
        });
    }

    @Subcommand("persistent")
    public void onPersistentCommand(Player player, String key, String value) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(JustUtils.getInstance(), key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        player.sendMessage(Messages.success("Item Modify", "Successfully added persistent data with key %s and value %s to item!", key, value));
    }

    @Subcommand("enchant")
    public void onEnchant(Player player, String enchantID, @Default("1") int level) {
        NamespacedKey key = NamespacedKey.minecraft(enchantID);
        Enchantment enchantment = Enchantment.getByKey(key);

        if (enchantment == null) {
            player.sendMessage(Messages.error("Item Modify", "Invalid enchant."));
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        item.addUnsafeEnchantment(enchantment, level);
    }

}
