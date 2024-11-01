package net.bitbylogic.apibylogic.util.item;

import com.google.common.collect.Lists;
import lombok.NonNull;
import net.bitbylogic.apibylogic.util.NumberUtil;
import net.bitbylogic.apibylogic.util.config.ConfigParser;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemStackConfigParser implements ConfigParser<ItemStack> {

    @Override
    public Optional<ItemStack> parseFrom(@NonNull ConfigurationSection section) {
        int amount = section.getInt("Amount", 1);
        ItemStack stack = new ItemStack(Material.valueOf(Formatter.format(section.getString("Material", "BARRIER"))), amount);
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return Optional.of(stack);
        }

        // Define the items name
        if (section.getString("Name") != null) {
            meta.setDisplayName(Formatter.format(section.getString("Name")));
        }

        List<String> lore = Lists.newArrayList();

        // Define the items lore
        section.getStringList("Lore").forEach(string ->
                lore.add(Formatter.format(string)));

        meta.setLore(lore);

        // Add flags to hide potion effects/attributes
        section.getStringList("Flags").forEach(flag -> {
            meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
        });

        // Add persistent data keys
        if (!section.getStringList("Custom-Data").isEmpty()) {
            section.getStringList("Custom-Data").forEach(data -> {
                String[] splitData = data.split(":");
                meta.getPersistentDataContainer().set(new NamespacedKey(splitData[0], splitData[1]), PersistentDataType.STRING, splitData[2]);
            });
        }

        // Make the item glow
        if (section.getBoolean("Glow")) {
            meta.addEnchant(Enchantment.UNBREAKING, 37, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (section.getBoolean("Hide-Tooltip")) {
            meta.setHideTooltip(true);
        }

        // If leather armor, apply dye color if defined
        if (stack.getType().name().startsWith("LEATHER_") && section.getString("Dye-Color") != null) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) stack.getItemMeta();
            java.awt.Color color = ChatColor.of(section.getString("Dye-Color", Formatter.colorToChatColor(Bukkit.getServer().getItemFactory().getDefaultLeatherColor()).toString())).getColor();
            leatherArmorMeta.setColor(Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()));
            stack.setItemMeta(leatherArmorMeta);
        }

        stack.setItemMeta(meta);

        // If the item is a potion, apply potion data
        if (stack.getType() == Material.SPLASH_POTION || stack.getType() == Material.POTION) {
            ConfigurationSection potionSection = section.getConfigurationSection("Potion-Data");

            if (potionSection != null) {
                boolean vanilla = potionSection.getBoolean("Vanilla", false);
                PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();
                String potionType = potionSection.getString("Type", "POISON");

                if (vanilla) {
                    potionMeta.setBasePotionType(PotionType.valueOf(potionType));
                } else {
                    potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(potionType), potionSection.getInt("Duration", 20), potionSection.getInt("Amplifier", 1) - 1), true);
                }

                stack.setItemMeta(potionMeta);
            }
        }

        if (stack.getType() == Material.TIPPED_ARROW) {
            PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();
            potionMeta.setBasePotionType(PotionType.valueOf(section.getString("Arrow-Type", "POISON")));
            stack.setItemMeta(potionMeta);
        }

        // If the item is a player head, apply skin
        if (section.getString("Skull-Name") != null && stack.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
            skullMeta.setOwner(Formatter.format(section.getString("Skull-Name", "Notch")));
            stack.setItemMeta(skullMeta);
        }

        if (section.getString("Skull-URL") != null) {
            SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
            PlayerProfile skullProfile = Bukkit.createPlayerProfile("Notch");
            PlayerTextures textures = skullProfile.getTextures();
            textures.clear();
            try {
                textures.setSkin(URI.create(section.getString("Skull-URL")).toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            skullProfile.setTextures(textures);
            skullMeta.setOwnerProfile(skullProfile);
            stack.setItemMeta(skullMeta);
        }

        // Used for resourcepacks, to display custom models
        if (section.getInt("Model-Data") != 0) {
            ItemMeta updatedMeta = stack.getItemMeta();
            updatedMeta.setCustomModelData(section.getInt("Model-Data"));
            stack.setItemMeta(updatedMeta);
        }

        ItemMeta updatedMeta = stack.getItemMeta();

        // Apply enchantments
        section.getStringList("Enchantments").forEach(enchant -> {
            String[] data = enchant.split(":");
            NamespacedKey key = NamespacedKey.minecraft(data[0].trim());
            Enchantment enchantment = Enchantment.getByKey(key);
            int level = 0;

            if (NumberUtil.isNumber(data[1])) {
                level = Integer.parseInt(data[1]);
            }

            if (enchantment == null) {
                Bukkit.getLogger().warning(String.format("[APIByLogic] (ItemStackUtil): Skipped enchantment '%s', invalid enchant.", enchant));
                return;
            }

            updatedMeta.addEnchant(enchantment, level, true);
        });

        stack.setItemMeta(updatedMeta);

        return Optional.of(stack);
    }

    @Override
    public ConfigurationSection parseTo(@NonNull ConfigurationSection section, @NonNull ItemStack itemStack) {
        section.set("Material", itemStack.getType().name());
        section.set("Amount", itemStack.getAmount());

        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) {
            return section;
        }

        if (meta.hasDisplayName()) {
            section.set("Name", Formatter.reverseColors(meta.getDisplayName()));
        }

        if (meta.hasLore() && meta.getLore() != null) {
            List<String> plainLore = new ArrayList<>();
            meta.getLore().forEach(loreLine -> plainLore.add(Formatter.reverseColors(loreLine)));
            section.set("Lore", plainLore);
        }

        List<String> flags = new ArrayList<>();
        meta.getItemFlags().forEach(itemFlag -> flags.add(itemFlag.name()));

        if (!flags.isEmpty()) {
            section.set("Flags", flags);
        }

        List<String> customData = new ArrayList<>();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        for (NamespacedKey key : dataContainer.getKeys()) {
            if (!dataContainer.has(key, PersistentDataType.STRING)) {
                continue;
            }

            customData.add(key.getNamespace() + ":" + key.getKey() + ":" + dataContainer.get(key, PersistentDataType.STRING));
        }

        if (!customData.isEmpty()) {
            section.set("Custom-Data", customData);
        }

        if (meta.hasEnchant(Enchantment.UNBREAKING)
                && meta.getEnchantLevel(Enchantment.UNBREAKING) == 37
                && meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            section.set("Glow", true);
        }

        if (meta.isHideTooltip()) {
            section.set("Hide-Tooltip", true);
        }

        if (meta instanceof LeatherArmorMeta leatherArmorMeta
                && !leatherArmorMeta.getColor().equals(Bukkit.getServer().getItemFactory().getDefaultLeatherColor())) {
            section.set("Dye-Color", Formatter.colorToChatColor(leatherArmorMeta.getColor()));
        }

        if (meta instanceof PotionMeta potionMeta
                && (itemStack.getType() == Material.POTION
                || itemStack.getType() == Material.SPLASH_POTION
                || itemStack.getType() == Material.LINGERING_POTION)) {
            if (potionMeta.getBasePotionType() != null) {
                section.set("Potion-Data.Vanilla", true);
                section.set("Potion-Data.Type", potionMeta.getBasePotionType().name());
            } else if (potionMeta.getCustomEffects().size() > 1) {
                PotionEffect effect = potionMeta.getCustomEffects().getFirst();
                section.set("Potion-Data.Type", effect.getType().getKey().getKey());
                section.set("Duration", effect.getDuration());
                section.set("Amplifier", effect.getAmplifier() + 1);
            }
        }

        if (meta instanceof PotionMeta potionMeta && potionMeta.getBasePotionType() != null
                && itemStack.getType() == Material.TIPPED_ARROW) {
            section.set("Arrow-Type", potionMeta.getBasePotionType().name());
        }

        if (meta instanceof SkullMeta skullMeta && skullMeta.getOwnerProfile() != null
                && skullMeta.getOwnerProfile().getTextures().getSkin() != null) {
            section.set("Skull-URL", skullMeta.getOwnerProfile().getTextures().getSkin().toString());
        }

        if (meta.hasCustomModelData()) {
            section.set("Model-Data", meta.getCustomModelData());
        }

        List<String> enchants = new ArrayList<>();

        meta.getEnchants().forEach((enchantment, integer) -> {
            enchants.add(enchantment.getKey().getKey() + ":" + integer);
        });

        if (!enchants.isEmpty()) {
            section.set("Enchantments", enchants);
        }

        return section;
    }

}
