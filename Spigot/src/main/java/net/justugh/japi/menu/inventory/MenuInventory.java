package net.justugh.japi.menu.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;

@Getter @Setter
@AllArgsConstructor
public class MenuInventory {

    private final Inventory inventory;
    private String title;

}
