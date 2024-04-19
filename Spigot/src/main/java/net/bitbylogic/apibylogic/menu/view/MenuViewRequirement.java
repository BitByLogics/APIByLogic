package net.bitbylogic.apibylogic.menu.view;

import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuItem;
import org.bukkit.inventory.Inventory;

public interface MenuViewRequirement {

    boolean canView(Inventory inventory, MenuItem item, Menu menu);

}
