package net.bitbylogic.apibylogic.menu.view.internal;

import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import net.bitbylogic.apibylogic.menu.view.MenuViewRequirement;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class NextPageViewRequirement implements MenuViewRequirement {

    @Override
    public boolean canView(@NotNull Inventory inventory, @NotNull MenuItem item, @NotNull Menu menu) {
        return menu.getInventories().size() > 1 && menu.getInventoryIndex(inventory) < menu.getInventories().size() - 1;
    }

}
