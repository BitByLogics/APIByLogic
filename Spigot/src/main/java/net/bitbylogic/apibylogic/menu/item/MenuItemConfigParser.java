package net.bitbylogic.apibylogic.menu.item;

import lombok.NonNull;
import net.bitbylogic.apibylogic.menu.action.MenuClickActionType;
import net.bitbylogic.apibylogic.util.config.ConfigParser;
import net.bitbylogic.apibylogic.util.item.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MenuItemConfigParser implements ConfigParser<MenuItem> {

    @Override
    public Optional<MenuItem> parseFrom(@NonNull ConfigurationSection section) {
        String id = section.getName();
        ItemStack item = ItemStackUtil.getFromConfig(section);
        MenuItem menuItem = new MenuItem(id).item(item).updatable(section.getBoolean("Update", false));

        if (!section.getStringList("Actions").isEmpty()) {
            HashMap<MenuClickActionType, String> internalActions = new HashMap<>();
            section.getStringList("Actions").forEach(action -> {
                String[] data = action.split(":");
                MenuClickActionType type = MenuClickActionType.parseType(data[0]);
                internalActions.put(type, data[1]);
            });

            menuItem.withInternalActions(internalActions);
        }

        ConfigurationSection metaDataSection = section.getConfigurationSection("Metadata");

        if (metaDataSection != null) {
            for (String metaKey : metaDataSection.getKeys(false)) {
                menuItem.withMetadata(metaKey, metaDataSection.get(metaKey, ""));
            }
        }

        if (section.getBoolean("Filler", false)) {
            menuItem.filler(true);
        }

        if (!section.getIntegerList("Slots").isEmpty()) {
            menuItem.withSlots(section.getIntegerList("Slots"));
            menuItem.setLocked(true);
            return Optional.of(menuItem);
        }

        int slot = section.getInt("Slot", -1);

        if (slot != -1) {
            menuItem.withSlot(slot);
        }

        menuItem.setLocked(true);
        return Optional.of(menuItem);
    }

    @Override
    public ConfigurationSection parseTo(@NonNull ConfigurationSection section, @NonNull MenuItem menuItem) {
        if(menuItem.isUpdatable()) {
            section.set("Update", true);
        }

        List<String> actions = new ArrayList<>();
        menuItem.getInternalActions().forEach((menuClickActionType, s) -> {
            actions.add(menuClickActionType.name() + ":" + s);
        });

        if(!actions.isEmpty()) {
            section.set("Actions", actions);
        }

        menuItem.getMetadata().forEach((s, object) -> {
            section.set("Metadata." + s, object);
        });

        if(menuItem.isFiller()) {
            section.set("Filler", true);
        }

        if(menuItem.getItem() != null) {
            ItemStackUtil.saveToConfig(section, menuItem.getItem());
        }

        if(menuItem.getSlots().isEmpty()) {
            return section;
        }

        if(menuItem.getSlots().size() == 1) {
            section.set("Slot", menuItem.getSlots().getFirst());
            return section;
        }

        section.set("Slots", menuItem.getSlots());
        return section;
    }

}
