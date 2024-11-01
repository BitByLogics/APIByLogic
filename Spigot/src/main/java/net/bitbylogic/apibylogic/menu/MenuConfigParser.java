package net.bitbylogic.apibylogic.menu;

import lombok.NonNull;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import net.bitbylogic.apibylogic.util.config.ConfigParser;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuConfigParser implements ConfigParser<Menu> {

    @Override
    public Optional<Menu> parseFrom(@NonNull ConfigurationSection section) {
        MenuBuilder builder = new MenuBuilder(
                section.getName(),
                Formatter.format(section.getString("Title", "Inventory")),
                section.getInt("Size", 9)
        );

        MenuData data = builder.getData();

        data.getValidSlots().addAll(section.getIntegerList("Valid-Slots"));

        ConfigurationSection metaDataSection = section.getConfigurationSection("Metadata");

        if (metaDataSection != null) {
            for (String metaKey : metaDataSection.getKeys(false)) {
                data.getMetadata().put(metaKey, metaDataSection.get(metaKey));
            }
        }

        section.getStringList("Flags").forEach(flag -> {
            data.withFlag(MenuFlag.valueOf(flag.toUpperCase()));
        });

        ConfigurationSection itemsSection = section.getConfigurationSection("Items");

        if(itemsSection == null) {
            return Optional.of(builder.build());
        }

        for (String identifier : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(identifier);

            if(itemSection == null) {
                continue;
            }

            MenuItem.getFromConfig(itemSection).ifPresent(builder::withItem);
        }

        return Optional.of(builder.build());
    }

    @Override
    public ConfigurationSection parseTo(@NonNull ConfigurationSection section, @NonNull Menu menu) {
        section.set("Title", menu.getTitle());
        section.set("Size", menu.getSize());

        MenuData menuData = menu.getData();

        if(!menuData.getValidSlots().isEmpty()) {
            section.set("Valid-Slots", menuData.getValidSlots());
        }

        menuData.getMetadata().forEach((s, object) -> {
            section.set("Metadata." + s, object);
        });

        List<String> flags = new ArrayList<>();
        menuData.getFlags().forEach(menuFlag -> flags.add(menuFlag.name()));

        if(!flags.isEmpty()) {
            section.set("Flags", flags);
        }

        if(menu.getItems().isEmpty() && menuData.getItemStorage().isEmpty()) {
            return section;
        }

        ConfigurationSection itemsSection = section.createSection("Items");

        menu.getItems().forEach(menuItem -> menuItem.saveToConfig(itemsSection));
        menuData.getItemStorage().forEach(menuItem -> menuItem.saveToConfig(itemsSection));

        return section;
    }

}
