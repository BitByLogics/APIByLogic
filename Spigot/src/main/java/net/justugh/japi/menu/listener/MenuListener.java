package net.justugh.japi.menu.listener;

import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (inventory == null || !(inventory.getHolder() instanceof Menu)) {
            return;
        }

        event.setCancelled(true);

        Menu menu = (Menu) inventory.getHolder();

        menu.getItem(inventory, event.getSlot()).ifPresent(menuItem -> {
            menuItem.onClick(event);
        });
    }

    @EventHandler
    public void onTransfer(InventoryMoveItemEvent event) {
        Inventory inventory = event.getDestination();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onExternalClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        if (event.getClickedInventory() == event.getInventory()) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.EXTERNAL_INPUT));

        if (menu.getData().getExternalClickAction() == null) {
            return;
        }

        menu.getData().getExternalClickAction().onClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        if (menu.getData().getCloseAction() == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> ((Player) event.getPlayer()).updateInventory(), 1);
        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> menu.getData().getCloseAction().onClose(event), 2);
    }

}
