package net.bitbylogic.apibylogic.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryUtil {

    /**
     * Check whether an inventory has space.
     *
     * @param inventory The inventory to check.
     * @return Whether the inventory has space.
     */
    public static boolean hasSpace(Inventory inventory) {
        return hasSpace(inventory, null, new ArrayList<>());
    }

    /**
     * Check whether an inventory has space for
     * an ItemStack.
     *
     * @param inventory The inventory to check.
     * @param itemStack The ItemStack to check space for, nullable.
     * @return Whether the inventory has space.
     */
    public static boolean hasSpace(Inventory inventory, @Nullable ItemStack itemStack) {
        return hasSpace(inventory, itemStack, new ArrayList<>());
    }

    /**
     * Check whether an inventory has space for
     * an ItemStack.
     *
     * @param inventory  The inventory to check.
     * @param itemStack  The ItemStack to check space for, nullable.
     * @param validSlots The slots that are valid to check, nullable.
     * @return Whether the inventory has space.
     */
    public static boolean hasSpace(Inventory inventory, @Nullable ItemStack itemStack, @Nullable List<Integer> validSlots) {
        int maxStackSize = itemStack == null ? -1 : itemStack.getMaxStackSize();
        int availableSpace = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (validSlots != null && !validSlots.isEmpty() && !validSlots.contains(i)) {
                continue;
            }

            ItemStack content = inventory.getItem(i);

            if (content == null || content.getType() == Material.AIR) {
                availableSpace += (maxStackSize == -1 ? 64 : maxStackSize);
                continue;
            }

            if (maxStackSize == -1 || !ItemStackUtil.isSimilar(itemStack, content, true, true, true)) {
                continue;
            }

            if (content.getAmount() >= maxStackSize) {
                continue;
            }

            availableSpace += content.getMaxStackSize() - content.getAmount();
        }

        if (itemStack != null) {
            return availableSpace > 0 && availableSpace >= itemStack.getAmount();
        }

        return availableSpace > 0;
    }

    /**
     * Get the amount of space an inventory has left.
     *
     * @param inventory The inventory to check.
     * @return Whether the inventory has space.
     */
    public static int getAvailableSpace(Inventory inventory) {
        return getAvailableSpace(inventory, null, new ArrayList<>());
    }

    /**
     * Get the amount of space an inventory has left.
     *
     * @param inventory The inventory to check.
     * @param itemStack The ItemStack to check space for, nullable.
     * @return Whether the inventory has space.
     */
    public static int getAvailableSpace(Inventory inventory, @Nullable ItemStack itemStack) {
        return getAvailableSpace(inventory, itemStack, new ArrayList<>());
    }

    /**
     * Get the amount of space an inventory has left.
     *
     * @param inventory  The inventory to check.
     * @param itemStack  The ItemStack to check space for, nullable.
     * @param validSlots The slots that are valid to check, nullable.
     * @return Whether the inventory has space.
     */
    public static int getAvailableSpace(Inventory inventory, @Nullable ItemStack itemStack, @Nullable List<Integer> validSlots) {
        int maxStackSize = itemStack == null ? -1 : itemStack.getMaxStackSize();
        int availableSpace = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (validSlots != null && !validSlots.isEmpty() && !validSlots.contains(i)) {
                continue;
            }

            ItemStack content = inventory.getItem(i);

            if (content == null || content.getType() == Material.AIR) {
                availableSpace += (maxStackSize == -1 ? 64 : maxStackSize);
                continue;
            }

            if (maxStackSize == -1 || !ItemStackUtil.isSimilar(itemStack, content, true, true, true)) {
                continue;
            }

            if (content.getAmount() >= maxStackSize) {
                continue;
            }

            availableSpace += content.getMaxStackSize() - content.getAmount();
        }

        return availableSpace;
    }

    /**
     * Get next valid slot for an Inventory.
     * Returns -1 for no available slots.
     *
     * @param inventory The inventory to check.
     * @return The next available slot.
     */
    public static int getNextAvailableSlot(Inventory inventory) {
        return getNextAvailableSlot(inventory, null, null);
    }

    /**
     * Get next valid slot for an ItemStack, or in general.
     * Returns -1 for no available slots.
     *
     * @param inventory The inventory to check.
     * @param itemStack The ItemStack to check space for, nullable.
     * @return The next available slot.
     */
    public static int getNextAvailableSlot(Inventory inventory, @Nullable ItemStack itemStack) {
        return getNextAvailableSlot(inventory, itemStack, null);
    }

    /**
     * Get next valid slot for an ItemStack, or in general.
     * Returns -1 for no available slots.
     *
     * @param inventory  The inventory to check.
     * @param itemStack  The ItemStack to check space for, nullable.
     * @param validSlots The slots to check for, nullable.
     * @return The next available slot.
     */
    public static int getNextAvailableSlot(Inventory inventory, @Nullable ItemStack itemStack, @Nullable List<Integer> validSlots) {
        if (!hasSpace(inventory, itemStack, validSlots)) {
            return -1;
        }

        int maxStackSize = itemStack == null ? -1 : itemStack.getMaxStackSize();

        for (int i = 0; i < inventory.getSize(); i++) {
            if (validSlots != null && !validSlots.isEmpty() && !validSlots.contains(i)) {
                continue;
            }

            ItemStack content = inventory.getItem(i);

            if (content == null || content.getType() == Material.AIR) {
                return i;
            }

            if (maxStackSize == -1 || !ItemStackUtil.isSimilar(itemStack, content, true, true, true)) {
                continue;
            }

            if (content.getAmount() >= maxStackSize) {
                continue;
            }

            return i;
        }

        return -1;
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

    /**
     * Add an ItemStack to an Inventory if space is available.
     *
     * @param inventory The inventory to add to.
     * @param itemStack The ItemStack to add.
     */
    public static void addItem(Inventory inventory, ItemStack itemStack) {
        addItem(inventory, itemStack, null);
    }

    /**
     * Add an ItemStack to an Inventory if space is available.
     *
     * @param inventory  The inventory to add to.
     * @param itemStack  The ItemStack to add.
     * @param validSlots The valid slots to check, nullable.
     */
    public static void addItem(Inventory inventory, ItemStack itemStack, @Nullable List<Integer> validSlots) {
        if (!hasSpace(inventory, itemStack, validSlots)) {
            return;
        }

        ItemStack clonedStack = itemStack.clone();
        int maxStackSize = itemStack.getMaxStackSize();

        for (int i = 0; i < inventory.getSize(); i++) {
            if (validSlots != null && !validSlots.isEmpty() && !validSlots.contains(i)) {
                continue;
            }

            if (clonedStack == null) {
                break;
            }

            ItemStack content = inventory.getItem(i);

            if (content == null || content.getType() == Material.AIR) {
                ItemStack newItem;

                if (clonedStack.getAmount() > maxStackSize) {
                    newItem = clonedStack.clone();
                    newItem.setAmount(maxStackSize);
                    clonedStack.setAmount(clonedStack.getAmount() - maxStackSize);
                } else {
                    newItem = clonedStack.clone();
                    clonedStack = null;
                }

                inventory.setItem(i, newItem);
                continue;
            }

            if (maxStackSize == -1 || !ItemStackUtil.isSimilar(itemStack, content, true, true, true)) {
                continue;
            }

            if (content.getAmount() >= maxStackSize) {
                continue;
            }

            int maxAvailable = maxStackSize - content.getAmount();
            int newAmount = Math.min(maxAvailable, clonedStack.getAmount());
            content.setAmount(content.getAmount() + newAmount);

            if (clonedStack.getAmount() - newAmount <= 0) {
                clonedStack = null;
            } else {
                clonedStack.setAmount(clonedStack.getAmount() - newAmount);
            }
        }
    }

}
