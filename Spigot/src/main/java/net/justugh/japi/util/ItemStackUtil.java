package net.justugh.japi.util;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.locale.LocaleLanguage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Set;

public class ItemStackUtil {

    /**
     * Create an ItemStack object from a configuration
     * section.
     *
     * @param section      The configuration section.
     * @param placeholders Placeholders to replace in the name/lore.
     * @return New ItemStack instance.
     */
    public static ItemStack getItemStackFromConfig(ConfigurationSection section, Placeholder... placeholders) {
        int amount = section.getInt("Amount") == 0 ? 1 : section.getInt("Amount");
        ItemStack stack = new ItemStack(Material.valueOf(Format.format(section.getString("Material", "BARRIER"), placeholders)), amount);
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return null;
        }

        if (section.getString("Name") != null) {
            meta.setDisplayName(Format.format(section.getString("Name"), placeholders));
        }

        List<String> lore = Lists.newArrayList();

        section.getStringList("Lore").forEach(string ->
                lore.add(Format.format(string, placeholders)));

        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES);

        if (section.getBoolean("Glow")) {
            stack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if(stack.getType().name().startsWith("LEATHER_") && section.getString("Dye-Color") != null) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) stack.getItemMeta();
            java.awt.Color color = ChatColor.of(section.getString("Dye-Color")).getColor();
            leatherArmorMeta.setColor(Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()));
            stack.setItemMeta(leatherArmorMeta);
        }

        stack.setItemMeta(meta);

        if (section.getString("Skull-Name") != null && stack.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
            skullMeta.setOwner(Format.format(section.getString("Skull-Name"), placeholders));
            stack.setItemMeta(skullMeta);
        }

        section.getStringList("Enchantments").forEach(enchant -> {
            String[] data = enchant.split(":");
            NamespacedKey key = NamespacedKey.minecraft(data[0]);
            Enchantment enchantment = Enchantment.getByKey(key);
            int level = 0;

            if (NumberUtil.isNumber(data[1])) {
                level = Integer.parseInt(data[1]);
            }

            if (enchantment == null) {
                Bukkit.getLogger().warning(String.format("[JustAPI] (ItemStackUtil): Skipped enchantment '%s', invalid enchant.", enchant));
                return;
            }

            stack.addUnsafeEnchantment(enchantment, level);
        });

        return stack;
    }

    /**
     * Get an ItemStack's vanilla name.
     *
     * @param item The ItemStack whose vanilla name to retrieve.
     * @return ItemStack's Vanilla Name.
     */
    public static String getVanillaName(ItemStack item) {
        return Format.format("&f" + LocaleLanguage.a().a(CraftItemStack.asNMSCopy(item).getItem().getName()));
    }

    /**
     * Update an ItemStack's name & lore with color
     * codes & the provided placeholders.
     *
     * @param item         The ItemStack to update.
     * @param placeholders The placeholders to replace.
     */
    public static void updateItem(ItemStack item, Placeholder... placeholders) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Format.format(meta.getDisplayName(), placeholders));

        if (meta.hasLore() && meta.getLore() != null) {
            List<String> lore = meta.getLore();
            List<String> updatedLore = Lists.newArrayList();

            lore.forEach(string -> updatedLore.add(Format.format(string, placeholders)));
            meta.setLore(updatedLore);
        }

        item.setItemMeta(meta);
    }

    /**
     * Merge ItemStack's lore into a main
     * ItemStack's lore.
     *
     * @param main        The main ItemStack.
     * @param otherStacks The other ItemStacks.
     */
    public static void mergeLore(ItemStack main, ItemStack... otherStacks) {
        if (main.getItemMeta() == null || main.getItemMeta().getLore() == null) {
            return;
        }

        ItemMeta meta = main.getItemMeta();
        List<String> mainLore = meta.hasLore() ? meta.getLore() : Lists.newArrayList();

        for (ItemStack otherItem : otherStacks) {
            if (otherItem.getItemMeta() == null || otherItem.getItemMeta().getLore() == null) {
                continue;
            }

            if (!otherItem.getItemMeta().hasLore()) {
                continue;
            }

            mainLore.addAll(otherItem.getItemMeta().getLore());
        }

        meta.setLore(mainLore);
        main.setItemMeta(meta);
    }

    /**
     * Merge all ItemStack lore into
     * a single list.
     *
     * @param items The ItemStacks.
     * @return A compiled lore list.
     */
    public static List<String> getMergedLore(ItemStack... items) {
        List<String> lore = Lists.newArrayList();

        for (ItemStack item : items) {
            if (item.getItemMeta() == null || item.getItemMeta().getLore() == null) {
                continue;
            }

            if (!item.getItemMeta().hasLore()) {
                continue;
            }

            lore.addAll(item.getItemMeta().getLore());
        }

        return lore;
    }

    /**
     * Check whether two ItemStacks are similar.
     *
     * @param item         ItemStack to compare.
     * @param otherItem    The ItemStack to compare it to.
     * @param compareFlags Whether to compare the ItemStack's flags.
     * @param compareName  Whether to compare the ItemStack's names.
     * @param compareLore  Whether to compare the ItemStack's lore.
     * @return Whether the ItemStacks are similar.
     */
    public static boolean isSimilar(ItemStack item, ItemStack otherItem, boolean compareFlags, boolean compareName, boolean compareLore) {
        if (item.getType() != otherItem.getType()) {
            return false;
        }

        ItemMeta mainMeta = item.getItemMeta();
        ItemMeta otherMeta = otherItem.getItemMeta();

        if (compareFlags) {
            if (!flagsMatch(item, otherItem)) {
                return false;
            }
        }

        if (compareName && (mainMeta != null && otherMeta != null)) {
            if (!mainMeta.getDisplayName().equalsIgnoreCase(otherMeta.getDisplayName())) {
                return false;
            }
        }

        if (compareLore && (mainMeta != null && otherMeta != null)) {
            return loreMatches(item, otherItem);
        }

        return true;
    }

    /**
     * Check whether two ItemStack's flags match.
     *
     * @param item      ItemStack to compare.
     * @param otherItem The ItemStack to compare it to.
     * @return Whether the flags match.
     */
    public static boolean flagsMatch(ItemStack item, ItemStack otherItem) {
        if (item.getItemMeta() == null || otherItem.getItemMeta() == null) {
            return false;
        }

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

    /**
     * Check whether two ItemStack's lore matches.
     *
     * @param item      ItemStack to compare.
     * @param otherItem The ItemStack to compare it to.
     * @return Whether the lore matches.
     */
    public static boolean loreMatches(ItemStack item, ItemStack otherItem) {
        if (item.getItemMeta() == null || otherItem.getItemMeta() == null) {
            return false;
        }

        if (!item.getItemMeta().hasLore() && !otherItem.getItemMeta().hasLore()) {
            return true;
        }

        List<String> itemLore = item.getItemMeta().getLore();
        List<String> otherItemLore = otherItem.getItemMeta().getLore();

        if (itemLore == null || otherItemLore == null) {
            return false;
        }

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

    /**
     * Check whether a spawner item's spawn
     * type matches another spawner's type.
     *
     * @param item      ItemStack to compare.
     * @param otherItem The ItemStack to compare it to.
     * @return Whether the spawn type matches.
     */
    public static boolean spawnerMatches(ItemStack item, ItemStack otherItem) {
        if (item.getType() != Material.SPAWNER || otherItem.getType() != Material.SPAWNER) {
            return false;
        }

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        BlockStateMeta otherMeta = (BlockStateMeta) otherItem.getItemMeta();

        if (meta == null || otherMeta == null) {
            return false;
        }

        return ((CreatureSpawner) meta.getBlockState()).getSpawnedType() != ((CreatureSpawner) otherMeta.getBlockState()).getSpawnedType();
    }

}
