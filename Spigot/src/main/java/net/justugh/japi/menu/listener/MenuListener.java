package net.justugh.japi.menu.listener;

import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuFlag;
import net.justugh.japi.util.InventoryUpdate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();
        Inventory bottomInventory = view.getBottomInventory();

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(topInventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) topInventory.getHolder();

        if (event.isShiftClick() && event.getClickedInventory() != topInventory) {
            event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
            return;
        }

        if (event.getClickedInventory() == bottomInventory) {
            event.setCancelled(!menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION));
            return;
        }

        if (event.getClickedInventory() != topInventory) {
            return;
        }

        if (!menu.getItem(topInventory, event.getSlot()).isPresent() && (event.getCursor() == null || event.getCursor().getType() == Material.AIR)) {
            return;
        }

        event.setCancelled(menu.getItem(topInventory, event.getSlot()).isPresent() || !menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));

        menu.getItems(topInventory, event.getSlot()).forEach(menuItem -> {
            if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(topInventory, menuItem, menu))) {
                return;
            }

            if (menuItem.getClickRequirements().stream().anyMatch(requirement -> !requirement.canClick((Player) event.getWhoClicked()))) {
                return;
            }

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
    public void onDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        Inventory inventory = event.getInventory();
        Inventory bottomInventory = view.getBottomInventory();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        if (inventory == bottomInventory && menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION)) {
            return;
        }

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        menu.getActivePlayers().add(event.getPlayer().getUniqueId());

        if (menu.getUpdateTask() != null) {
            return;
        }

        menu.startUpdateTask();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryUpdate.getLastSentTitle().remove(event.getPlayer().getUniqueId());
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        menu.getActivePlayers().remove(event.getPlayer().getUniqueId());

        if (menu.getData().getCloseAction() == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> ((Player) event.getPlayer()).updateInventory(), 1);
        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> menu.getData().getCloseAction().onClose(event), 2);
    }

}
