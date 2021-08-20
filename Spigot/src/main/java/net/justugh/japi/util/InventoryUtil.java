package net.justugh.japi.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryUtil {

    public static boolean hasSpace(Inventory inventory, ItemStack itemStack) {
        ItemStack item = new ItemStack(itemStack.clone());
        Inventory testInv = Bukkit.createInventory(null, inventory instanceof PlayerInventory ? 36 : inventory.getSize());

        Arrays.stream(inventory.getContents().clone()).filter(Objects::nonNull).forEach(invItem -> {
            ItemStack clonedItem = invItem.clone();
            ItemMeta meta = clonedItem.getItemMeta();
            meta.setDisplayName(ThreadLocalRandom.current().nextInt() + "");
            meta.setLore(Collections.singletonList(ThreadLocalRandom.current().nextInt() + ""));
            clonedItem.setItemMeta(meta);
            testInv.addItem(clonedItem);
        });

        // Potion Fix
        if (itemStack.getType() == Material.POTION) {
            if (itemStack.getAmount() > 36) {
                return false;
            }

            int openSlots = 0;

            while (testInv.firstEmpty() != -1) {
                testInv.addItem(new ItemStack(Material.OAK_LOG, 64));
                openSlots++;
            }

            return openSlots >= itemStack.getAmount();
        }

        return testInv.addItem(item).isEmpty();
    }

    public static boolean hasItems(Inventory inventory, ItemStack itemStack) {
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
            if (item != null && item.getType() != Material.AIR) {
                if (ItemStackUtil.isSimilarWithLore(item, itemStack)) {
                    if (itemStack.getType() == Material.SPAWNER) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        BlockStateMeta otherMeta = (BlockStateMeta) itemStack.getItemMeta();

                        if (((CreatureSpawner) meta.getBlockState()).getSpawnedType() != ((CreatureSpawner) otherMeta.getBlockState()).getSpawnedType()) {
                            continue;
                        }

                        found += item.getAmount();
                    } else if (item.getDurability() == itemStack.getDurability()) {
                        found += item.getAmount();
                    }
                }
            }
        }
        if (found >= itemStack.getAmount()) {
            return true;
        }

        return false;
    }

    public static int getItems(Inventory inventory, ItemStack itemStack) {
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
            if (item != null && item.getType() != Material.AIR) {
                if (ItemStackUtil.isSimilarWithLore(item, itemStack)) {
                    if (itemStack.getType() == Material.SPAWNER) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        BlockStateMeta otherMeta = (BlockStateMeta) itemStack.getItemMeta();

                        if (((CreatureSpawner) meta.getBlockState()).getSpawnedType() != ((CreatureSpawner) otherMeta.getBlockState()).getSpawnedType()) {
                            continue;
                        }

                        found += item.getAmount();
                    } else if (item.getDurability() == itemStack.getDurability()) {
                        found += item.getAmount();
                    }
                }
            }
        }
        if (found >= itemStack.getAmount()) {
            return found;
        }

        return 0;
    }

}
