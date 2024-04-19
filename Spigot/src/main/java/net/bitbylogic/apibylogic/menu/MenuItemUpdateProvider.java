package net.bitbylogic.apibylogic.menu;

import org.bukkit.inventory.ItemStack;

public interface MenuItemUpdateProvider {

    ItemStack requestItem(MenuItem menuItem);

}
