package net.justugh.japi.menu;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@Setter
public class MenuItem {

    private final String identifier;
    private final boolean updatable;

    private ItemStack item;
    private MenuItemUpdateProvider itemUpdateProvider;

    private Inventory sourceInventory;
    private List<Integer> slots;
    private MenuAction action;

    public MenuItem(String identifier, ItemStack item, List<Integer> slots, boolean updatable, MenuAction action) {
        this(identifier, item, slots, updatable);
        this.action = action;
    }

    public MenuItem(String identifier, ItemStack item, List<Integer> slots, boolean updatable) {
        this.identifier = identifier;
        this.item = item;
        this.slots = slots;
        this.updatable = updatable;
        this.action = null;
    }

    public MenuItem addSlot(int slot) {
        slots.add(slot);
        return this;
    }

    public void onClick(InventoryClickEvent event) {
        if (action == null) {
            return;
        }

        action.onClick(event);
    }

    public MenuItem clone() {
        return new MenuItem(identifier, item.clone(), Lists.newArrayList(), updatable);
    }
}
