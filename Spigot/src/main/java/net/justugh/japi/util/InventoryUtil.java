package net.justugh.japi.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryUtil {

    /**
     * Check whether an inventory has space for
     * an ItemStack.
     *
     * @param inventory The inventory to check.
     * @param itemStack The ItemStack to try to add.
     * @return Whether the inventory has space.
     */
    public static boolean hasSpace(Inventory inventory, ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        ItemStack item = new ItemStack(itemStack.clone());
        int availableSpace = 0;

        for (ItemStack content : inventory.getContents()) {
            if (content == null || content.getType() == Material.AIR) {
                availableSpace += item.getMaxStackSize();
                continue;
            }

            if (!ItemStackUtil.isSimilar(item, content, true, true, true)) {
                continue;
            }

            availableSpace += content.getMaxStackSize() - content.getAmount();
        }

        return availableSpace > 0;
    }

    /**
     * Check whether an inventory has an ItemStack.
     *
     * @param inventory         Inventory to check.
     * @param itemStack         ItemStack to check for.
     * @param amount            The amount to check for.
     * @param compareFlags      Whether to match the flags.
     * @param compareName       Whether to match the name.
     * @param compareLore       Whether to match the lore.
     * @param compareDurability Whether to match the durability.
     * @return Whether the inventory has the items.
     */
    public static boolean hasItems(Inventory inventory, ItemStack itemStack, int amount, boolean compareFlags, boolean compareName, boolean compareLore, boolean compareDurability) {
        if (itemStack == null) {
            return false;
        }

        AtomicInteger currentSlot = new AtomicInteger(0);
        Inventory testInv = Bukkit.createInventory(null, inventory instanceof PlayerInventory ? 36 : inventory.getSize());

        ItemStack[] contents = inventory.getContents().clone();

        // Get rid of armor
        if (inventory instanceof PlayerInventory) {
            contents[contents.length - 2] = null;
            contents[contents.length - 3] = null;
            contents[contents.length - 4] = null;
            contents[contents.length - 5] = null;
        }

        Arrays.stream(contents).filter(Objects::nonNull).forEach(invItem -> {
            ItemStack clonedItem = invItem.clone();
            testInv.setItem(currentSlot.getAndIncrement(), clonedItem);
        });

        int found = 0;
        for (ItemStack item : testInv.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!ItemStackUtil.isSimilar(item, itemStack, compareFlags, compareName, compareLore)) {
                continue;
            }

            if (itemStack.getType() == Material.SPAWNER && ItemStackUtil.spawnerMatches(itemStack, item)) {
                found += item.getAmount();
                continue;
            }

            if (compareDurability && item.getDurability() == itemStack.getDurability()) {
                found += item.getAmount();
                continue;
            }

            found += item.getAmount();
        }

        return found >= amount;
    }

    /**
     * Check whether an inventory has an ItemStack.
     * <p>
     * NOTE: This is a very basic matching. It does
     * not take name, lore, flags or durability
     * into account. For that use {@link #hasItems(Inventory, ItemStack, int, boolean, boolean, boolean, boolean)}.
     *
     * @param inventory Inventory to check.
     * @param itemStack ItemStack to check for.
     * @param amount    The amount to check for.
     * @return Whether the inventory has the items.
     */
    public static boolean hasItems(Inventory inventory, ItemStack itemStack, int amount) {
        return hasItems(inventory, itemStack, amount, false, false, false, false);
    }

    /**
     * Get how many of a certain ItemStack is in the
     * provided inventory.
     *
     * @param inventory         Inventory to check.
     * @param itemStack         ItemStack to check for.
     * @param compareFlags      Whether to match the flags.
     * @param compareName       Whether to match the name.
     * @param compareLore       Whether to match the lore.
     * @param compareDurability Whether to match the durability.
     * @return The amount of items discovered.
     */
    public static int getItems(Inventory inventory, ItemStack itemStack, boolean compareFlags, boolean compareName, boolean compareLore, boolean compareDurability) {
        if (itemStack == null) {
            return 0;
        }

        AtomicInteger currentSlot = new AtomicInteger(0);
        Inventory testInv = Bukkit.createInventory(null, inventory instanceof PlayerInventory ? 36 : inventory.getSize());

        ItemStack[] contents = inventory.getContents().clone();

        // Get rid of armor
        if (inventory instanceof PlayerInventory) {
            contents[contents.length - 2] = null;
            contents[contents.length - 3] = null;
            contents[contents.length - 4] = null;
            contents[contents.length - 5] = null;
        }

        Arrays.stream(contents).filter(Objects::nonNull).forEach(invItem -> {
            ItemStack clonedItem = invItem.clone();
            testInv.setItem(currentSlot.getAndIncrement(), clonedItem);
        });

        int found = 0;
        for (ItemStack item : testInv.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!ItemStackUtil.isSimilar(item, itemStack, compareFlags, compareName, compareLore)) {
                continue;
            }

            if (itemStack.getType() == Material.SPAWNER && ItemStackUtil.spawnerMatches(itemStack, item)) {
                found += item.getAmount();
                continue;
            }

            if (compareDurability && item.getDurability() == itemStack.getDurability()) {
                found += item.getAmount();
                continue;
            }

            found += item.getAmount();
        }

        return found;
    }

    /**
     * Get how many of a certain ItemStack is in the
     * provided inventory.
     * <p>
     * NOTE: This is a very basic matching. It does
     * not take name, lore, flags or durability
     * into account. For that use {@link #getItems(Inventory, ItemStack, boolean, boolean, boolean, boolean)}.
     *
     * @param inventory Inventory to check.
     * @param itemStack ItemStack to check for.
     * @return The amount of items discovered.
     */
    public static int getItems(Inventory inventory, ItemStack itemStack) {
        return getItems(inventory, itemStack, false, false, false, false);
    }

}
