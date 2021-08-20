package net.justugh.japi.util;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_16_R3.LocaleLanguage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ItemStackUtil {

    public static ItemStack getItemStackFromConfig(ConfigurationSection config, Placeholder... placeholders) {
        int amount = config.getInt("Amount") == 0 ? 1 : config.getInt("Amount");
        ItemStack stack = new ItemStack(Material.valueOf(Format.format(config.getString("Material", "BARRIER"), placeholders)), amount);
        ItemMeta meta = stack.getItemMeta();

        if (config.getString("Name") != null) {
            meta.setDisplayName(Format.format(config.getString("Name"), placeholders));
        }

        List<String> lore = Lists.newArrayList();

        config.getStringList("Lore").forEach(string -> {
            lore.add(Format.format(string, placeholders));
        });

        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES);

        if (config.getBoolean("Glow")) {
            stack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        stack.setItemMeta(meta);

        if (config.getString("Skull-Name") != null) {
            SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
            skullMeta.setOwner(Format.format(config.getString("Skull-Name"), placeholders));
            stack.setItemMeta(skullMeta);
        }

        config.getStringList("Enchantments").forEach(enchant -> stack.addUnsafeEnchantment(Objects.requireNonNull(Enchantment.getByName(enchant.split(":")[0])), Integer.parseInt(enchant.split(":")[1])));

        return stack;
    }

    public static String getVanillaName(ItemStack item) {
        return Format.format("&f" + LocaleLanguage.a().a(CraftItemStack.asNMSCopy(item).getItem().getName()));
    }

    public static void updateItem(ItemStack item, Placeholder... placeholders) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(Format.format(meta.getDisplayName(), placeholders));

        List<String> lore = item.getItemMeta().getLore();
        List<String> updatedLore = Lists.newArrayList();

        lore.forEach(string -> {
            updatedLore.add(Format.format(string, placeholders));
        });

        meta.setLore(updatedLore);
        item.setItemMeta(meta);
    }

    public static void mergeLore(ItemStack main, ItemStack... otherStacks) {
        ItemMeta meta = main.getItemMeta();
        List<String> mainLore = meta.hasLore() ? meta.getLore() : Lists.newArrayList();

        for (ItemStack otherItem : otherStacks) {
            if (!otherItem.getItemMeta().hasLore()) {
                continue;
            }

            mainLore.addAll(otherItem.getItemMeta().getLore());
        }

        meta.setLore(mainLore);
        main.setItemMeta(meta);
    }

    public static List<String> getMergedLore(ItemStack... items) {
        List<String> lore = Lists.newArrayList();

        for (ItemStack item : items) {
            if (!item.getItemMeta().hasLore()) {
                continue;
            }

            lore.addAll(item.getItemMeta().getLore());
        }

        return lore;
    }

    public static boolean isSimilar(ItemStack item, ItemStack otherItem) {
        if (item.getType() != otherItem.getType()) {
            return false;
        }

        if (item.hasItemMeta() && !otherItem.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().getDisplayName().equalsIgnoreCase(otherItem.getItemMeta().getDisplayName());
    }

    public static boolean isSimilarWithLore(ItemStack item, ItemStack otherItem) {
        if (item.getType() != otherItem.getType()) {
            return false;
        }

        if (item.hasItemMeta() && !otherItem.hasItemMeta()) {
            return false;
        }

        if (!item.getItemMeta().hasDisplayName() && otherItem.getItemMeta().hasDisplayName()) {
            return false;
        }

        if (item.getItemMeta().hasDisplayName() && otherItem.getItemMeta().hasDisplayName()) {
            if (!item.getItemMeta().getDisplayName().equalsIgnoreCase(otherItem.getItemMeta().getDisplayName())) {
                return false;
            }
        }

        if (!item.getItemMeta().hasLore() && otherItem.getItemMeta().hasLore()) {
            return false;
        }

        if (item.getItemMeta().hasLore() && otherItem.getItemMeta().hasLore()) {
            if (!loreMatches(item, otherItem)) {
                return false;
            }
        }

        if (!flagsMatch(item, otherItem)) {
            return false;
        }

        return true;
    }

    public static boolean flagsMatch(ItemStack item, ItemStack otherItem) {
        Set<ItemFlag> itemFlags = item.getItemMeta().getItemFlags();
        Set<ItemFlag> otherItemFlags = otherItem.getItemMeta().getItemFlags();

        for (ItemFlag itemFlag : itemFlags) {
            if (!otherItemFlags.contains(itemFlag)) {
                return false;
            }
        }

        for (ItemFlag itemFlag : otherItemFlags) {
            if (!itemFlags.contains(itemFlag)) {
                return false;
            }
        }

        return true;
    }

    public static boolean loreMatches(ItemStack item, ItemStack otherItem) {
        List<String> itemLore = item.getItemMeta().getLore();
        List<String> otherItemLore = otherItem.getItemMeta().getLore();

        for (String loreItem : itemLore) {
            if (!otherItemLore.contains(loreItem)) {
                return false;
            }
        }

        for (String loreItem : otherItemLore) {
            if (!itemLore.contains(loreItem)) {
                return false;
            }
        }

        return true;
    }

}
