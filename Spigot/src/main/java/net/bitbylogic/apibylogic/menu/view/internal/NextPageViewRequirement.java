package net.bitbylogic.apibylogic.menu.view.internal;

import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuItem;
import net.bitbylogic.apibylogic.menu.view.MenuViewRequirement;
import org.bukkit.inventory.Inventory;

public class NextPageViewRequirement implements MenuViewRequirement {

    @Override
    public boolean canView(Inventory inventory, MenuItem item, Menu menu) {
        return menu.getInventories().size() > 1 && menu.getInventoryIndex(inventory) < menu.getInventories().size() - 1;
    }

}
