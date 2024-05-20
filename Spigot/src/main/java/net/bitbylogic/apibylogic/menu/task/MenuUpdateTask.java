package net.bitbylogic.apibylogic.menu.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuItem;
import net.bitbylogic.apibylogic.menu.inventory.MenuInventory;
import net.bitbylogic.apibylogic.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@RequiredArgsConstructor
public class MenuUpdateTask {

    private final Menu menu;

    private int taskId;

    @Getter
    private boolean active;

    public void startTask() {
        if (active) {
            return;
        }

        active = true;
        taskId = Bukkit.getScheduler().runTaskTimer(APIByLogic.getInstance(), this::pushUpdates, 0, 5).getTaskId();
    }

    public void cancelTask() {
        if (!active) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        active = false;
    }

    private void pushUpdates() {
        if (menu.getData().getMaxInventories() != -1 && !menu.getInventories().isEmpty()) {
            Inventory finalInventory = menu.getInventories().get(menu.getInventories().size() - 1).getInventory();

            if (!InventoryUtil.hasSpace(finalInventory, null, menu.getData().getValidSlots())) {
                menu.generateNewInventory().ifPresent(menu.getInventories()::add);
            }
        }

        for (MenuItem menuItem : menu.getItems()) {
            if (menuItem.getSourceInventories().isEmpty()) {
                continue;
            }

            menuItem.getSourceInventories().forEach(inventory -> {
                if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, menu))) {
                    menuItem.getSlots().forEach(slot -> inventory.setItem(slot, null));
                    return;
                }

                List<Integer> slots = menuItem.getSlots();

                if (menuItem.getItem() == null) {
                    slots.forEach(slot -> inventory.setItem(slot, null));
                    return;
                }

                ItemStack item = menuItem.getItem().clone();
                menu.updateItemMeta(item);

                slots.forEach(slot -> {
                    if (inventory.getItem(slot) == null) {
                        inventory.setItem(slot, item);
                    }

                    if (inventory.getItem(slot).getType() != item.getType()) {
                        inventory.setItem(slot, item);
                    }
                });

                if (!menuItem.isUpdatable()) {
                    return;
                }

                ItemStack updatedItem = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);
                menu.updateItemMeta(updatedItem);

                slots.forEach(slot -> inventory.setItem(slot, updatedItem));
            });
        }

        for (MenuInventory menuInventory : menu.getInventories()) {
            Inventory inventory = menuInventory.getInventory();

            if (menu.getData().getFillerItem() != null && menu.getData().getFillerItem().getItem().getType() != Material.AIR) {
                MenuItem fillerItem = menu.getData().getFillerItem();

                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) != null || menu.getData().getValidSlots().contains(i)) {
                        continue;
                    }

                    inventory.setItem(i, fillerItem.getItem());
                }
            }
        }
    }

}
