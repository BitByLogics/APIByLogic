package net.bitbylogic.apibylogic.menu.view;

import lombok.NonNull;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import org.bukkit.inventory.Inventory;

public interface MenuViewRequirement {

    boolean canView(@NonNull Inventory inventory, @NonNull MenuItem item, @NonNull Menu menu);

}
