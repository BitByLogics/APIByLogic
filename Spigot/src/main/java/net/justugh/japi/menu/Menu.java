package net.justugh.japi.menu;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.util.ItemStackUtil;
import net.justugh.japi.util.Placeholder;
import net.justugh.japi.util.StringModifier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
public class Menu implements InventoryHolder {

    private String id;
    private final String title;
    private final int size;

    private final List<MenuItem> items;
    private MenuData data;

    private final List<Inventory> inventories;
    private final HashMap<UUID, List<Inventory>> userMenus;

    public Menu(String title, Rows rows) {
        this(title, rows.getSize());
    }

    public Menu(String title, int size) {
        this.title = title;
        this.size = size;

        this.items = new ArrayList<>();
        this.data = new MenuData();
        this.inventories = new ArrayList<>();
        this.userMenus = new HashMap<>();

        JustAPIPlugin.getInstance().getMenuManager().getActiveMenus().add(this);

        Bukkit.getScheduler().runTaskTimer(JustAPIPlugin.getInstance(), () -> {
            pushUpdates();
            pushUserUpdates();
        }, 0, 5);
    }

    //TODO: Introduce a way to fully remove MenuItem
    private void pushUpdates() {
        for (Inventory inventory : inventories) {
            Iterator<MenuItem> slotIterator = items.iterator();

            while (slotIterator.hasNext()) {
                MenuItem menuItem = slotIterator.next();

                if (!menuItem.getSourceInventories().contains(inventory)) {
                    menuItem.getSourceInventories().add(inventory);
                }

                List<Integer> slots = menuItem.getSlots();

                if (menuItem.getItem() == null) {
                    slots.forEach(slot -> inventory.setItem(slot, null));
                    continue;
                }

                ItemStack item = menuItem.getItem().clone();
                updateItemMeta(item);

                slots.forEach(slot -> {
                    if (inventory.getItem(slot) == null) {
                        inventory.setItem(slot, item);
                    }

                    if (inventory.getItem(slot).getType() != item.getType()) {
                        inventory.setItem(slot, item);
                    }
                });

                if (!menuItem.isUpdatable()) {
                    continue;
                }

                ItemStack updatedItem = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);
                updateItemMeta(updatedItem);

                slots.forEach(slot -> inventory.setItem(slot, updatedItem));
            }
        }
    }

    private void pushUserUpdates() {
        userMenus.forEach((uuid, allInventories) -> {
            for (Inventory inventory : allInventories) {
                Iterator<MenuItem> slotIterator = items.iterator();

                while (slotIterator.hasNext()) {
                    MenuItem menuItem = slotIterator.next();

                    if (!menuItem.getSourceInventories().contains(inventory)) {
                        continue;
                    }

                    List<Integer> slots = menuItem.getSlots();

                    if (menuItem.getItem() == null) {
                        slots.forEach(slot -> inventory.setItem(slot, null));
                        continue;
                    }

                    ItemStack item = menuItem.getItem().clone();
                    updateItemMeta(item);

                    slots.forEach(slot -> {
                        if (inventory.getItem(slot) == null) {
                            inventory.setItem(slot, item);
                        }

                        if (inventory.getItem(slot).getType() != item.getType()) {
                            inventory.setItem(slot, item);
                        }
                    });

                    if (!menuItem.isUpdatable()) {
                        continue;
                    }

                    ItemStack updatedItem = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);
                    updateUserItemMeta(menuItem, Bukkit.getOfflinePlayer(uuid), updatedItem);

                    slots.forEach(slot -> inventory.setItem(slot, updatedItem));
                }
            }
        });
    }

    private void updateItemMeta(ItemStack item) {
        List<StringModifier> placeholders = new ArrayList<>();

        placeholders.addAll(data.getModifiers());
        data.getPlaceholderProviders().forEach(placeholder -> placeholders.add(new Placeholder(placeholder.getIdentifier(), placeholder.getValue())));

        ItemStackUtil.updateItem(item, placeholders.toArray(new StringModifier[]{}));
    }

    private void updateUserItemMeta(MenuItem menuItem, OfflinePlayer player, ItemStack item) {
        List<StringModifier> placeholders = new ArrayList<>();

        placeholders.addAll(data.getModifiers());
        data.getPlaceholderProviders().forEach(placeholder -> placeholders.add(new Placeholder(placeholder.getIdentifier(), placeholder.getValue())));
        data.getUserPlaceholderProviders().forEach(userPlaceholderProvider -> placeholders.add(new Placeholder(userPlaceholderProvider.getIdentifier(), userPlaceholderProvider.getValue(menuItem, player))));

        ItemStackUtil.updateItem(item, placeholders.toArray(new StringModifier[]{}));
    }

    /**
     * Add an item to the Menu.
     *
     * @param item The item to add.
     * @return The Menu instance.
     */
    public Menu addItem(MenuItem item) {
        items.add(item);
        return this;
    }

    /**
     * Set an item in the Menu.
     *
     * @param slot The slot to set.
     * @param item The item to set.
     * @return The Menu instance.
     */
    public Menu setItem(int slot, MenuItem item) {
        item.addSlot(slot);
        items.add(item);
        return this;
    }

    /**
     * Get a MenuItem instance by its identifier.
     *
     * @param identifier The MenuItem identifier.
     * @return The optional MenuItem instance.
     */
    public Optional<MenuItem> getItem(String identifier) {
        return items.stream().filter(item -> item.getIdentifier() != null && item.getIdentifier().equalsIgnoreCase(identifier)).findFirst();
    }

    /**
     * Get a MenuItem instance by its slot.
     *
     * @param slot The MenuItem slot.
     * @return The optional MenuItem instance.
     */
    public Optional<MenuItem> getItem(Inventory inventory, int slot) {
        return items.stream().filter(item -> item.getSlots().contains(slot) && (item.getSourceInventories().contains(inventory))).findFirst();
    }

    private List<Inventory> generateInventories() {
        List<Inventory> generatedInventories = new ArrayList<>();
        List<Integer> validSlots = data.getValidSlots();

        if (validSlots.isEmpty()) {
            for (int i = 0; i < (items.size() > size - 1 ? size - 9 : size); i++) {
                validSlots.add(i);
            }
        }

        int pages = items.size() / (size - 9.0d) % 1 == 0 ? items.size() / (size - 9) : items.size() / (size - 9) + 1;

        AtomicReference<List<Integer>> availableSlots = new AtomicReference<>(new ArrayList<>(validSlots));
        AtomicReference<Inventory> inventory = new AtomicReference<>(Bukkit.createInventory(this, size, title.replace("%page%", "1").replace("%pages%", pages + "")));
        generatedInventories.add(inventory.get());

        List<MenuItem> itemCache = new ArrayList<>();

        items.forEach(item -> {
            if (data.hasFlag(MenuFlag.PAGE_ITEMS)) {
                Inventory currentInventory = inventory.get();
                MenuItem nextPageItem = getData().getItemFromStorage("Next-Page-Item").clone();
                MenuItem previousPageItem = getData().getItemFromStorage("Previous-Page-Item").clone();
                nextPageItem.addSourceInventory(currentInventory);

                nextPageItem.setSlots((List<Integer>) getData().getMetaData().get("Next-Page-Slots"));
                nextPageItem.getSlots().forEach(slot -> currentInventory.setItem(slot, nextPageItem.getItem().clone()));

                previousPageItem.addSourceInventory(currentInventory);
                previousPageItem.setSlots((List<Integer>) getData().getMetaData().get("Previous-Page-Slots"));

                previousPageItem.getSlots().forEach(slot -> currentInventory.setItem(slot, previousPageItem.getItem().clone()));

                nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
                previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));

                itemCache.add(nextPageItem);
                itemCache.add(previousPageItem);
            }

            if (availableSlots.get().isEmpty()) {
                Inventory currentInventory = inventory.get();

                MenuItem nextPageItem = getData().getItemFromStorage("Next-Page-Item").clone();
                MenuItem previousPageItem = getData().getItemFromStorage("Previous-Page-Item").clone();
                nextPageItem.addSourceInventory(currentInventory);

                previousPageItem.addAction((event) -> event.getWhoClicked().openInventory(currentInventory));

                nextPageItem.setSlots((List<Integer>) getData().getMetaData().get("Next-Page-Slots"));
                nextPageItem.getSlots().forEach(slot -> currentInventory.setItem(slot, nextPageItem.getItem().clone()));

                generatedInventories.add(currentInventory);

                Inventory newInventory = Bukkit.createInventory(this, size, title.replace("%page%", generatedInventories.size() + "").replace("%pages%", pages + ""));
                inventory.set(newInventory);
                previousPageItem.addSourceInventory(newInventory);
                previousPageItem.setSlots((List<Integer>) getData().getMetaData().get("Previous-Page-Slots"));

                nextPageItem.addAction((event) -> event.getWhoClicked().openInventory(newInventory));

                previousPageItem.getSlots().forEach(slot -> newInventory.setItem(slot, previousPageItem.getItem().clone()));
                availableSlots.set(new ArrayList<>(validSlots));

                nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
                previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));

                itemCache.add(nextPageItem);
                itemCache.add(previousPageItem);
            }

            Inventory targetInventory = inventory.get();

            ItemStack itemStack = item.getItemUpdateProvider() == null ? item.getItem().clone() : item.getItemUpdateProvider().requestItem(item);
            updateItemMeta(itemStack);

            if (!item.getSlots().isEmpty()) {
                item.addSourceInventory(targetInventory);
                item.getSlots().forEach(slot -> {
                    availableSlots.get().removeAll(Collections.singletonList(slot));
                    targetInventory.setItem(slot, itemStack);
                });

                return;
            }

            int slot = availableSlots.get().get(0);
            availableSlots.get().removeAll(Collections.singletonList(slot));

            item.addSourceInventory(targetInventory);
            item.getSlots().add(slot);

            targetInventory.setItem(slot, itemStack);
        });

        items.addAll(itemCache);

        if (data.getFillerItem() != null && data.getFillerItem().getItem().getType() != Material.AIR) {
            generatedInventories.forEach(targetInventory -> {
                MenuItem fillerItem = data.getFillerItem();

                while (targetInventory.firstEmpty() != -1) {
                    int slot = targetInventory.firstEmpty();
                    targetInventory.setItem(slot, fillerItem.getItem());
                }
            });
        }

        return generatedInventories;
    }

    /**
     * Get the built Inventory.
     *
     * @return The build Inventory.
     */
    @Override
    public Inventory getInventory() {
        if (!inventories.isEmpty()) {
            return inventories.get(0);
        }

        inventories.addAll(generateInventories());
        return inventories.get(0);
    }

    /**
     * Get a user Menu from the Menu.
     * <p>
     * This is useful for many cases, mainly
     * being the fact that it can be modified
     * and updated.
     *
     * @return User Menu.
     */
    public Inventory getUserMenu(Player player) {
        if (userMenus.containsKey(player.getUniqueId())) {
            userMenus.get(player.getUniqueId()).forEach(inv -> new ArrayList<>(inv.getViewers()).forEach(HumanEntity::closeInventory));
            userMenus.remove(player.getUniqueId());
        }

        List<Inventory> newInventories = generateInventories();
        userMenus.put(player.getUniqueId(), newInventories);
        return newInventories.get(0);
    }

    public Inventory getGlobalMenu() {
        if (inventories.isEmpty()) {
            getInventory();
        }

        return inventories.get(0);
    }

}
