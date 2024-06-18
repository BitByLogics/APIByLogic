package net.bitbylogic.logicutils.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import net.bitbylogic.apibylogic.util.message.Formatter;
import net.bitbylogic.logicutils.LogicUtils;
import net.bitbylogic.logicutils.config.Messages;
import net.bitbylogic.logicutils.util.PersistentDataUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("itemdebug")
@CommandPermission("logicutils.command.itemdebug")
public class ItemDebugComand extends BaseCommand {

    @Default
    public void onDefault(Player sender, @Default("1") int page) {
        List<String> data = new ArrayList<>();

        sender.sendMessage(Formatter.autoFormat(Messages.testMessage, "TACO"));

        ItemStack item = sender.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            sender.sendMessage(Formatter.error("Item Debug", "You must hold a valid item."));
            return;
        }

        data.add(Formatter.dottedMessage("Material", item.getType().name()));
        data.add(Formatter.dottedMessage("Amount", item.getAmount() + ""));

        ItemMeta meta = item.getItemMeta();

        if (meta.hasDisplayName()) {
            data.add(Formatter.dottedMessage("Name", meta.getDisplayName()));
        }

        if (meta.hasLore()) {
            meta.getLore().forEach(s -> data.add(Formatter.dottedMessage("Lore", s)));
        }

        if (!item.getEnchantments().isEmpty()) {
            item.getEnchantments().forEach((enchantment, integer) -> data.add(Formatter.dottedMessage("Enchantment", enchantment.getName() + ", " + integer)));
        }

        if (!meta.getItemFlags().isEmpty()) {
            meta.getItemFlags().forEach(flag -> data.add(Formatter.dottedMessage("Flag", flag.name())));
        }

        if (meta.hasCustomModelData()) {
            data.add(Formatter.dottedMessage("Model Data", meta.getCustomModelData() + ""));
        }

        if (meta.hasAttributeModifiers()) {
            meta.getAttributeModifiers().forEach((attribute, attributeModifier) -> data.add(Formatter.dottedMessage("Attribute", attribute.name() + ", " + attributeModifier.getName() + ", " + attributeModifier.getAmount())));
        }

        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
            meta.getPersistentDataContainer().getKeys().forEach(key -> {
                PersistentDataUtil.getValues(meta.getPersistentDataContainer(), key).forEach(o -> {
                    data.add(Formatter.dottedMessage("Persistent Data", key.getNamespace() + ", " + key.getKey() + ", " + o));
                });
            });
        }

        sender.sendMessage(Formatter.getPagedList("Item Debug", data, page));
    }

    @Subcommand("hat")
    public void onHatCommand(Player player) {
        LogicUtils.getInstance().reloadConfig();
        LogicUtils.getInstance().getMessages().loadConfigData();

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            return;
        }

        player.getInventory().setItem(EquipmentSlot.HEAD, item);
    }

    @Subcommand("back")
    public void onBackCommand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            return;
        }

        player.getInventory().setItem(EquipmentSlot.CHEST, item);
    }

}
