package net.justugh.japi.menu;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.action.MenuClickActionType;
import net.justugh.japi.util.Format;
import net.justugh.japi.util.ItemStackUtil;
import net.justugh.japi.util.StringModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Getter
@NoArgsConstructor
public class MenuBuilder {

    private String id;
    private String title;
    private int size;

    private List<MenuItem> items;
    private MenuData data;

    private Menu menu;

    public MenuBuilder(String id, String title, Rows rows) {
        this(id, title, rows.getSize());
    }

    public MenuBuilder(String id, String title, int size) {
        this.id = id;
        this.title = title;
        this.size = size;

        this.items = new ArrayList<>();
        this.data = new MenuData();
    }

    public MenuBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public MenuBuilder setSize(int size) {
        this.size = size;
        return this;
    }

    public MenuBuilder addItem(MenuItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }

        items.add(item);
        return this;
    }

    public MenuBuilder setItems(List<MenuItem> items) {
        this.items = items;
        return this;
    }

    public MenuBuilder setData(MenuData data) {
        this.data = data;
        return this;
    }

    public Menu getMenu() {
        Preconditions.checkNotNull(title, "Invalid/Missing Title");
        Preconditions.checkState(size % 9 == 0, "Size must be multiple of 9");

        if (menu == null) {
            menu = new Menu(title, size);
        }

        menu.setData(data);
        menu.setId(id);

        menu.getItems().clear();
        items.forEach(menuItem -> {
            if (menuItem.getSlots().isEmpty()) {
                menu.getData().getItemStorage().add(menuItem);
            } else {
                menu.addItem(menuItem);
            }
        });

        return menu;
    }

    /**
     * Create a Menu from a configuration section.
     *
     * @param section   The ConfigurationSection.
     * @param modifiers The Placeholders.
     * @return This builder.
     */
    public MenuBuilder fromConfiguration(ConfigurationSection section, StringModifier... modifiers) {
        List<StringModifier> modifierList = new ArrayList<>(Arrays.asList(modifiers));

        id = section.getName();
        title = Format.format(Preconditions.checkNotNull(Format.format(section.getString("Title"), modifierList.toArray(new StringModifier[]{})), "Invalid/Missing Title"));
        size = section.getInt("Size");

        data = new MenuData();

        data.getModifiers().addAll(modifierList);
        data.getValidSlots().addAll(section.getIntegerList("Valid-Slots"));

        ConfigurationSection metaDataSection = section.getConfigurationSection("Metadata");

        if (metaDataSection != null) {
            for (String metaKey : metaDataSection.getKeys(false)) {
                data.getMetaData().put(metaKey, metaDataSection.get(metaKey));
            }
        }

        section.getStringList("Flags").forEach(flag -> {
            data.addFlag(MenuFlag.valueOf(flag.toUpperCase()));
        });

        menu = new Menu(title, size, data);

        ConfigurationSection itemSection = section.getConfigurationSection("Items");

        return loadItemsFromConfig(itemSection, modifierList.toArray(new StringModifier[]{}));
    }

    /**
     * Load Menu Items from a ConfigurationSection.
     *
     * @param section   The ConfigurationSection.
     * @param modifiers The Modifiers.
     * @return This builder.
     */
    public MenuBuilder loadItemsFromConfig(ConfigurationSection section, StringModifier... modifiers) {
        if (section == null) {
            return null;
        }

        items = new ArrayList<>();

        for (String identifier : section.getKeys(false)) {
            ItemStack item = ItemStackUtil.getItemStackFromConfig(section.getConfigurationSection(identifier));
            MenuItem menuItem = new MenuItem(identifier, item, new ArrayList<>(), section.getBoolean(identifier + ".Update", false));
            menuItem.setItemSection(section.getConfigurationSection(identifier));

            if (!section.getStringList(identifier + ".Actions").isEmpty()) {
                HashMap<MenuClickActionType, String> internalActions = new HashMap<>();
                section.getStringList(identifier + ".Actions").forEach(action -> {
                    String[] data = action.split(":");
                    MenuClickActionType type = MenuClickActionType.parseType(data[0]);
                    internalActions.put(type, data[1]);
                });
                menuItem.setInternalActions(internalActions);
            }

            ConfigurationSection metaDataSection = section.getConfigurationSection(identifier + ".Metadata");

            if (metaDataSection != null) {
                for (String metaKey : metaDataSection.getKeys(false)) {
                    menuItem.getMetaData().put(metaKey, metaDataSection.getString(metaKey));
                }
            }

            if (section.getBoolean(identifier + ".Filler", false)) {
                data.setFillerItem(menuItem);
                continue;
            }

            if (!section.getIntegerList(identifier + ".Slots").isEmpty()) {
                menuItem.getSlots().addAll(section.getIntegerList(identifier + ".Slots"));
            } else {
                int slot = section.getInt(identifier + ".Slot", -1);

                if (slot != -1) {
                    menuItem.addSlot(slot);
                }
            }

            items.add(menuItem);
        }

        return this;
    }

}
