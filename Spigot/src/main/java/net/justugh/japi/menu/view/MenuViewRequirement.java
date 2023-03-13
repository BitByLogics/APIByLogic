package net.justugh.japi.menu.view;

import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuItem;
import org.bukkit.inventory.Inventory;

public interface MenuViewRequirement {

    boolean canView(Inventory inventory, MenuItem item, Menu menu);

}
