package net.justugh.japi.util;

import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.action.ItemAction;
import net.justugh.japi.action.PlayerInteractAction;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack itemStack;

    private ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public ItemBuilder name(String name) {
        ItemMeta stackMeta = itemStack.getItemMeta();
        stackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        itemStack.setItemMeta(stackMeta);
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder lore(String... lore) {
        ItemMeta meta = itemStack.getItemMeta();
        List<String> newLore = meta.getLore();
        if (newLore == null) {
            newLore = new ArrayList<>();
        }

        for (String s : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', s));
        }

        meta.setLore(newLore);
        this.itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        for (String part : lore) {
            lore(part);
        }

        return this;
    }

    public ItemBuilder data(short data) {
        itemStack.setDurability(data);
        return this;
    }

    public ItemBuilder durability(short durability) {
        itemStack.setDurability(durability);
        return this;
    }

    public ItemBuilder modelData(int modelData) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setCustomModelData(modelData);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeAttributes() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder unbreakable() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setUnbreakable(true);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addFlag(ItemFlag flag) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(flag);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addAction(PlayerInteractAction action) {
        if (itemStack == null) {
            build();
        }

        String itemIdentifier = UUID.randomUUID().toString();
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(JustAPIPlugin.getInstance(), itemIdentifier), PersistentDataType.STRING, "");
        itemStack.setItemMeta(meta);
        action.setItemIdentifier(itemIdentifier);
        JustAPIPlugin.getInstance().getActionManager().trackItemAction(null, action);
        return this;
    }

    public <T, Z> ItemBuilder addPersistentData(String key, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(JustAPIPlugin.getInstance(), key), type, value);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder spawnerType(EntityType entityType) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(JustAPIPlugin.getInstance(), "justapi_spawner"), PersistentDataType.STRING, entityType.name());
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }

}