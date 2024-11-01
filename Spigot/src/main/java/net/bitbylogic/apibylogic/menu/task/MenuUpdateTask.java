package net.bitbylogic.apibylogic.menu.task;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuData;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import net.bitbylogic.apibylogic.menu.inventory.MenuInventory;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.inventory.InventoryUtil;
import net.bitbylogic.apibylogic.util.item.ItemStackUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class MenuUpdateTask {

    private final Menu menu;

    private int taskId = -1;

    public void startTask() {
        if (taskId != -1) {
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimer(APIByLogic.getInstance(), this::pushUpdates, 0, 5).getTaskId();
    }

    public void cancelTask() {
        if (taskId == -1) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private void pushUpdates() {
        if (menu.getData().getMaxInventories() != -1 && !menu.getInventories().isEmpty()) {
            Inventory finalInventory = menu.getInventories().getLast().getInventory();

            if (!InventoryUtil.hasSpace(finalInventory, null, menu.getData().getValidSlots())) {
                menu.generateNewInventory().ifPresent(menu.getInventories()::add);
            }
        }

        Iterator<MenuItem> storedItemIterator = menu.getData().getItemStorage().iterator();

        while(storedItemIterator.hasNext()) {
            MenuItem storedItem = storedItemIterator.next();

            if(storedItem.getSourceInventories().isEmpty() || storedItem.getSlots().isEmpty()) {
               continue;
            }

            menu.addItem(storedItem);
            storedItemIterator.remove();
        }

        Iterator<MenuItem> itemIterator = menu.getItems().iterator();

        while(itemIterator.hasNext()) {
            MenuItem menuItem = itemIterator.next();

            if (menuItem.getSourceInventories().isEmpty() || menuItem.getSlots().isEmpty()) {
                menu.getData().getItemStorage().add(menuItem);
                itemIterator.remove();
                continue;
            }

            menuItem.getSourceInventories().forEach(inventory -> {
                if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, menu))) {
                    menuItem.getSlots().forEach(slot -> inventory.setItem(slot, null));
                    return;
                }

                List<Integer> slots = menuItem.getSlots();

                if (menuItem.getItem() == null && menuItem.getItemUpdateProvider() == null) {
                    slots.forEach(slot -> inventory.setItem(slot, null));
                    return;
                }

                ItemStack item = menuItem.getItem().clone();
                updateItemMeta(menu, item);

                slots.forEach(slot -> {
                    if (inventory.getItem(slot) != null && inventory.getItem(slot).getType() == item.getType()) {
                        return;
                    }

                    inventory.setItem(slot, item);
                });

                if (!menuItem.isUpdatable()) {
                    return;
                }

                ItemStack updatedItem = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);
                updateItemMeta(menu, updatedItem);

                slots.forEach(slot -> inventory.setItem(slot, updatedItem));
            });
        }

        for (MenuInventory menuInventory : menu.getInventories()) {
            Inventory inventory = menuInventory.getInventory();

            menu.getData().getFillerItem().ifPresent(fillerItem -> {
                if(fillerItem.getItem() == null || fillerItem.getItem().getType().isAir()) {
                    return;
                }

                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) != null || menu.getData().getValidSlots().contains(i)) {
                        continue;
                    }

                    inventory.setItem(i, fillerItem.getItem());
                }
            });
        }
    }

    private void updateItemMeta(@NonNull Menu menu, @NonNull ItemStack item) {
        final MenuData data = menu.getData();

        if (data.getModifiers().isEmpty() && data.getPlaceholderProviders().isEmpty()) {
            return;
        }

        List<StringModifier> placeholders = new ArrayList<>(data.getModifiers());
        data.getPlaceholderProviders().forEach(placeholder -> placeholders.add(placeholder.asPlaceholder()));

        ItemStackUtil.updateItem(item, placeholders.toArray(new StringModifier[]{}));
    }

    public boolean isActive() {
        return taskId != -1;
    }

}
