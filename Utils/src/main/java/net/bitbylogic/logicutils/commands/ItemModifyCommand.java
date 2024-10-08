package net.bitbylogic.logicutils.commands;

import net.bitbylogic.apibylogic.acf.BaseCommand;
import net.bitbylogic.apibylogic.acf.annotation.CommandAlias;
import net.bitbylogic.apibylogic.acf.annotation.CommandPermission;
import net.bitbylogic.apibylogic.acf.annotation.Default;
import net.bitbylogic.apibylogic.acf.annotation.Subcommand;
import net.bitbylogic.apibylogic.util.RichTextUtil;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import net.bitbylogic.logicutils.LogicUtils;
import net.bitbylogic.logicutils.player.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
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
                Formatter.richFormat(Formatter.listHeader("Item Modify", "Valid Commands")),
                Formatter.richCommand("itemmodify name <name>", "Change the items name."),
                Formatter.richCommand("itemmodify lore <lore>", "Change the items lore."),
                Formatter.richCommand("itemmodify model <id>", "Change custom model data id."),
                Formatter.richCommand("itemmodify persistent <key> <value>", "Apply a persistent data value.")
        );
    }

    @Subcommand("model")
    public void onModelCommand(Player player, int id) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(id);
        item.setItemMeta(meta);
        player.sendMessage(Formatter.success("Item Modify", "Successfully set model id to %s!", id));
    }

    @Subcommand("name")
    public void onNameCommand(Player player, String name) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Formatter.format(name));
        item.setItemMeta(meta);
        player.sendMessage(Formatter.success("Item Modify", "Successfully set item name to %s!", name));
    }

    @Subcommand("lore")
    public void onLore(Player player, @Default("1") int page) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold a valid item."));
            return;
        }

        List<String> data = new ArrayList<>();

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            player.sendMessage(Formatter.error("Item Modify", "This item has no lore."));
            return;
        }

        int loreIndex = 0;
        for (String loreLine : meta.getLore()) {
            data.add(Formatter.dottedMessage("Lore #" + loreIndex++, loreLine));
        }

        player.sendMessage(Formatter.getPagedList("Item Lore", data, page));
    }

    @Subcommand("lore index")
    public void onLoreIndex(Player player, int index, String loreLine) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold a valid item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            player.sendMessage(Formatter.error("Item Modify", "This item has no lore."));
            return;
        }

        List<String> lore = meta.getLore();

        if (lore.size() - 1 < index) {
            player.sendMessage(Formatter.error("Item Modify", "That's an invalid index!"));
            return;
        }

        lore.set(index, Formatter.format(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
        player.sendMessage(Formatter.success("Item Modify", "Successfully updated lore index %s to %s!", index, loreLine));
    }

    @Subcommand("lore append")
    public void onLoreAppend(Player player, String loreLine) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold a valid item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            player.sendMessage(Formatter.error("Item Modify", "This item has no lore."));
            return;
        }

        List<String> lore = meta.getLore();

        lore.add(Formatter.format(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Subcommand("lore set")
    public void onSetLore(Player player, String lore) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        List<String> actualLore = new ArrayList<>();

        for (String loreLine : RichTextUtil.getRichText(lore, 0)) {
            actualLore.add(Formatter.format(loreLine));
        }

        ItemMeta meta = item.getItemMeta();
        meta.setLore(actualLore);
        item.setItemMeta(meta);

        player.spigot().sendMessage(Formatter.richFormat(Formatter.success("Item Modify", "Successfully set item lore to: ")));
        AtomicInteger position = new AtomicInteger();
        actualLore.forEach(loreLine -> {
            player.spigot().sendMessage(Formatter.richFormat(Formatter.listItem("#" + position.incrementAndGet(), loreLine)));
        });
    }

    @Subcommand("persistent")
    public void onPersistentCommand(Player player, String key, String value) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(LogicUtils.getInstance(), key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        player.sendMessage(Formatter.success("Item Modify", "Successfully added persistent data with key %s and value %s to item!", key, value));
    }

    @Subcommand("enchant")
    public void onEnchant(Player player, String enchantID, @Default("1") int level) {
        NamespacedKey key = NamespacedKey.minecraft(enchantID);
        Enchantment enchantment = Enchantment.getByKey(key);

        if (enchantment == null) {
            player.sendMessage(Formatter.error("Item Modify", "Invalid enchant."));
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        item.addUnsafeEnchantment(enchantment, level);
    }

    @Subcommand("flag add")
    public void onFlagAdd(Player player, ItemFlag flag) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(flag);
        item.setItemMeta(meta);
        player.sendMessage(Formatter.success("Item Modify", "Successfully added flag %s to item!", flag.name()));
    }

    @Subcommand("flag remove")
    public void onFlagRemove(Player player, ItemFlag flag) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(Formatter.error("Item Modify", "You must hold an item."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.removeItemFlags(flag);
        item.setItemMeta(meta);
        player.sendMessage(Formatter.success("Item Modify", "Successfully removed flag %s from item!", flag.name()));
    }

}
