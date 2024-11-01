package net.bitbylogic.apibylogic.menu;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.menu.action.MenuClickActionType;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.item.ItemStackUtil;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Getter
public class MenuBuilder {

    private final String id;

    private String title = "Inventory";
    private int size = 9;

    private List<MenuItem> items = new ArrayList<>();
    private MenuData data = new MenuData();

    private Menu menu;

    public MenuBuilder(@NonNull String id) {
        this.id = id;
    }

    public MenuBuilder(@NonNull String id, @NonNull String title, @NonNull MenuRows menuRows) {
        this(id, title, menuRows.getSize());
    }

    public MenuBuilder(@NonNull String id, @NonNull String title, int size) {
        this.id = id;
        this.title = title;
        this.size = size;
    }

    public MenuBuilder title(String title) {
        this.title = title;
        return this;
    }

    public MenuBuilder size(int size) {
        this.size = size;
        return this;
    }

    public MenuBuilder withItem(@NonNull MenuItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }

        if(item.getSlots().isEmpty()) {
            data.getItemStorage().add(item);
            return this;
        }

        items.add(item);
        return this;
    }

    public MenuBuilder withItems(List<MenuItem> items) {
        for (MenuItem item : items) {
            if(item.getSlots().isEmpty()) {
                data.getItemStorage().add(item);
                continue;
            }

            items.add(item);
        }
        return this;
    }

    public MenuBuilder data(MenuData data) {
        this.data = data;
        return this;
    }

    public Menu build() {
        Preconditions.checkNotNull(title, "Invalid title");
        Preconditions.checkState(size % 9 == 0, "Size must be multiple of 9");

        menu = new Menu(id, title, size, data);
        items.forEach(menuItem -> menu.addItem(menuItem));

        return menu;
    }

}
