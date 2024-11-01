package net.bitbylogic.apibylogic.menu;

import lombok.NonNull;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface MenuAction {

    void onClick(@NonNull InventoryClickEvent event);

}
