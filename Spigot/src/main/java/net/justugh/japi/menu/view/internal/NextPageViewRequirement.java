package net.justugh.japi.menu.view.internal;

import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuItem;
import net.justugh.japi.menu.view.MenuViewRequirement;
import org.bukkit.inventory.Inventory;

public class NextPageViewRequirement implements MenuViewRequirement {

    @Override
    public boolean canView(Inventory inventory, MenuItem item, Menu menu) {
        return menu.getInventories().size() > 1 && menu.getInventoryIndex(inventory) < menu.getInventories().size() - 1;
    }

}
