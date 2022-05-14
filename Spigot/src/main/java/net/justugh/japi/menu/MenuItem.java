package net.justugh.japi.menu;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.menu.action.MenuClickActionType;
import net.justugh.japi.menu.action.MenuClickRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class MenuItem {

    private final String identifier;

    private ItemStack item;
    private MenuItemUpdateProvider itemUpdateProvider;

    private Inventory sourceInventory;
    private List<Integer> slots;
    private HashMap<String, String> metaData;
    private boolean updatable;
    private MenuClickRequirement clickRequirement;
    private MenuAction action;
    private HashMap<MenuClickActionType, String> internalActions;

    public MenuItem(String identifier, ItemStack item, List<Integer> slots, boolean updatable, MenuAction action, HashMap<MenuClickActionType, String> internalActions) {
        this(identifier, item, slots, updatable);
        this.action = action;
        this.internalActions = internalActions;
    }

    public MenuItem(String identifier, ItemStack item, List<Integer> slots, boolean updatable, MenuAction action) {
        this(identifier, item, slots, updatable);
        this.action = action;
        this.internalActions = new HashMap<>();
    }

    public MenuItem(String identifier, ItemStack item, List<Integer> slots, boolean updatable) {
        this.identifier = identifier;
        this.item = item;
        this.slots = slots;
        this.updatable = updatable;
        this.action = null;
        this.internalActions = new HashMap<>();
        this.metaData = new HashMap<>();
    }

    public MenuItem addSlot(int slot) {
        slots.add(slot);
        return this;
    }

    public void onClick(InventoryClickEvent event) {
        if (clickRequirement != null && !clickRequirement.canClick((Player) event.getWhoClicked())) {
            return;
        }

        internalActions.keySet().forEach(action -> action.getAction().onClick(event, internalActions.get(action)));

        if (action == null) {
            return;
        }

        action.onClick(event);
    }

    public MenuItem clone() {
        return new MenuItem(identifier, item.clone(), Lists.newArrayList(), updatable);
    }
}
