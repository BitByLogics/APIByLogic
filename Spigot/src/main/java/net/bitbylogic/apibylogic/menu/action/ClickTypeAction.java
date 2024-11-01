package net.bitbylogic.apibylogic.menu.action;

import lombok.NonNull;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface ClickTypeAction {

    void onClick(@NonNull InventoryClickEvent event, @NonNull String args);

}
