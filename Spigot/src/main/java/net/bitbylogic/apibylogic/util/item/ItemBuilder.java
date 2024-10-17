package net.bitbylogic.apibylogic.util.item;

import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.action.PlayerInteractAction;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
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

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public ItemBuilder name(String name) {
        ItemMeta stackMeta = item.getItemMeta();
        stackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(stackMeta);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder lore(String... lore) {
        ItemMeta meta = item.getItemMeta();
        List<String> newLore = meta.getLore();
        if (newLore == null) {
            newLore = new ArrayList<>();
        }

        for (String s : lore) {
            newLore.add(Formatter.format(s));
        }

        meta.setLore(newLore);
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        for (String part : lore) {
            lore(part);
        }

        return this;
    }

    public ItemBuilder data(short data) {
        item.setDurability(data);
        return this;
    }

    public ItemBuilder durability(short durability) {
        item.setDurability(durability);
        return this;
    }

    public ItemBuilder modelData(int modelData) {
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(modelData);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeAttributes() {
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder unbreakable() {
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();

        if(meta == null) {
            return this;
        }

        meta.addItemFlags(flags);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addAction(PlayerInteractAction action) {
        if (item == null) {
            build();
        }

        String itemIdentifier = UUID.randomUUID().toString();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(APIByLogic.getInstance(), itemIdentifier), PersistentDataType.STRING, "");
        item.setItemMeta(meta);
        action.setItemIdentifier(itemIdentifier);
        APIByLogic.getInstance().getActionManager().trackAction(null, action);
        return this;
    }

    public <T, Z> ItemBuilder addPersistentData(String key, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = item.getItemMeta();

        if(meta == null) {
            return this;
        }

        meta.getPersistentDataContainer().set(new NamespacedKey(APIByLogic.getInstance(), key), type, value);
        item.setItemMeta(meta);
        return this;
    }

    public <T, Z> ItemBuilder addPersistentData(NamespacedKey namespacedKey, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = item.getItemMeta();

        if(meta == null) {
            return this;
        }

        meta.getPersistentDataContainer().set(namespacedKey, type, value);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder spawner(EntityType entityType) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(APIByLogic.getInstance(), "abl_spawner"), PersistentDataType.STRING, entityType.name());
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return item;
    }

}