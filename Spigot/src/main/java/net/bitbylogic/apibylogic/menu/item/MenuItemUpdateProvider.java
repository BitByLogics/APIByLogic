package net.bitbylogic.apibylogic.menu.item;

import org.bukkit.inventory.ItemStack;

public interface MenuItemUpdateProvider {

    ItemStack requestItem(MenuItem menuItem);

}
