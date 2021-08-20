package net.justugh.japi.menu;

import org.bukkit.event.inventory.InventoryCloseEvent;

public interface MenuCloseAction {

    void onClose(InventoryCloseEvent event);

}
