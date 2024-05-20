package net.bitbylogic.logicutils.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import net.bitbylogic.apibylogic.util.RichTextUtil;
import net.bitbylogic.apibylogic.util.message.Messages;
import net.bitbylogic.logicutils.LogicUtils;
import net.bitbylogic.logicutils.player.PlayerUtil;
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
@CommandPermission("logicutils.command.itemmodify")
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
    public void onLore(Player player, @Default("1") int page) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold a valid item."));
            return;
        }

        List<String> data = new ArrayList<>();

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            player.sendMessage(Messages.error("Item Modify", "This item has no lore."));
            return;
        }

        int loreIndex = 0;
        for (String loreLine : meta.getLore()) {
            data.add(Messages.dottedMessage("Lore #" + loreIndex++, loreLine));
        }

        player.sendMessage(Messages.getPagedList("Item Lore", data, page));
    }

    @Subcommand("lore index")
    public void onLoreIndex(Player player, int index, String loreLine) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold a valid item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            player.sendMessage(Messages.error("Item Modify", "This item has no lore."));
            return;
        }

        List<String> lore = meta.getLore();

        if (lore.size() - 1 < index) {
            player.sendMessage(Messages.error("Item Modify", "That's an invalid index!"));
            return;
        }

        lore.set(index, Messages.format(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
        player.sendMessage(Messages.success("Item Modify", "Successfully updated lore index %s to %s!", index, loreLine));
    }

    @Subcommand("lore append")
    public void onLoreAppend(Player player, String loreLine) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.error("Item Modify", "You must hold a valid item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            player.sendMessage(Messages.error("Item Modify", "This item has no lore."));
            return;
        }

        List<String> lore = meta.getLore();

        lore.add(Messages.format(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Subcommand("lore set")
    public void onSetLore(Player player, String lore) {
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
        meta.getPersistentDataContainer().set(new NamespacedKey(LogicUtils.getInstance(), key), PersistentDataType.STRING, value);
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
