package net.justugh.japi.menu;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.justugh.japi.util.Format;
import net.justugh.japi.util.ItemStackUtil;
import net.justugh.japi.util.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class MenuBuilder {

    private String title;
    private int size;

    private List<MenuItem> items;
    private MenuData data;

    private Menu menu;

    public MenuBuilder(String title, Rows rows) {
        this(title, rows.getSize());
    }

    public MenuBuilder(String title, int size) {
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

        menu = new Menu(title, size);
        menu.setData(data);

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
     * @param section      The ConfigurationSection.
     * @param placeholders The Placeholders.
     * @return This builder.
     */
    public MenuBuilder fromConfiguration(ConfigurationSection section, Placeholder... placeholders) {
        title = Format.format(Preconditions.checkNotNull(Format.format(section.getString("Title"), placeholders), "Invalid/Missing Title"));
        size = section.getInt("Size");

        menu = new Menu(title, size);
        data = new MenuData();

        ConfigurationSection metaDataSection = section.getConfigurationSection("Metadata");

        if (metaDataSection != null) {
            for (String metaKey : metaDataSection.getKeys(false)) {
                data.getMetaData().put(metaKey, metaDataSection.get(metaKey));
            }
        }

        ConfigurationSection itemSection = section.getConfigurationSection("Items");

        loadItemsFromConfig(itemSection, placeholders);

        if (section.getConfigurationSection("Filler-Item") != null) {
            ItemStack fillerItem = ItemStackUtil.getItemStackFromConfig(section.getConfigurationSection("Filler-Item"), placeholders);

            data.setFillerItem(new MenuItem("filler-item", fillerItem, new ArrayList<>(), false));
        }

        return this;
    }

    /**
     * Load Menu Items from a ConfigurationSection.
     *
     * @param section      The ConfigurationSection.
     * @param placeholders The Placeholders.
     * @return This builder.
     */
    public MenuBuilder loadItemsFromConfig(ConfigurationSection section, Placeholder... placeholders) {
        if (section == null) {
            return null;
        }

        items = new ArrayList<>();

        for (String identifier : section.getKeys(false)) {
            ItemStack item = ItemStackUtil.getItemStackFromConfig(section.getConfigurationSection(identifier), placeholders);
            MenuItem menuItem = new MenuItem(identifier, item, new ArrayList<>(), section.getBoolean(identifier + ".Update", false));

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

        items.forEach(menuItem -> {
            if (menuItem.getSlots().isEmpty()) {
                menu.getData().getItemStorage().add(menuItem);
            } else {
                menu.addItem(menuItem);
            }
        });

        return this;
    }

}
