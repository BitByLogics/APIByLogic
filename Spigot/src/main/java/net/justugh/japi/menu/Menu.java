package net.justugh.japi.menu;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.inventory.MenuInventory;
import net.justugh.japi.menu.placeholder.PlaceholderProvider;
import net.justugh.japi.menu.view.internal.NextPageViewRequirement;
import net.justugh.japi.menu.view.internal.PreviousPageViewRequirement;
import net.justugh.japi.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Getter
@Setter
public class Menu implements InventoryHolder, Cloneable {

    private String id;
    private final String title;
    private final int size;

    private final List<MenuItem> items;
    private MenuData data;

    private final List<MenuInventory> inventories;
    private final HashMap<UUID, List<MenuInventory>> userMenus;
    private final List<UUID> activePlayers;

    private BukkitTask updateTask;
    private long lastUpdateCheck;

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
        this.activePlayers = new ArrayList<>();

        JustAPIPlugin.getInstance().getMenuManager().getActiveMenus().add(this);

        startTitleUpdateTask();
        startUpdateTask();
    }

    public Menu(String id, String title, int size, List<MenuItem> items, MenuData data, List<MenuInventory> inventories, HashMap<UUID, List<MenuInventory>> userMenus) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.items = items;
        this.data = data;
        this.inventories = inventories;
        this.userMenus = userMenus;
        this.activePlayers = new ArrayList<>();

        JustAPIPlugin.getInstance().getMenuManager().getActiveMenus().add(this);

        startTitleUpdateTask();
        startUpdateTask();
    }

    public void startTitleUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(JustAPIPlugin.getInstance(), () -> {
            List<StringModifier> modifiers = new ArrayList<>();
            modifiers.addAll(data.getModifiers());
            modifiers.addAll(data.getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).collect(Collectors.toList()));

            Placeholder pagesPlaceholder = new Placeholder("%pages%", inventories.size() + "");
            modifiers.add(pagesPlaceholder);

            inventories.forEach(menuInventory -> {
                Inventory inventory = menuInventory.getInventory();

                Placeholder pagePlaceholder = new Placeholder("%page%", getInventoryIndex(inventory) + "");
                modifiers.add(pagePlaceholder);

                new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                    InventoryUpdate.updateInventory(JustAPIPlugin.getInstance(), (Player) viewer, Format.format(menuInventory.getTitle(),
                            modifiers.toArray(new StringModifier[]{})));
                });
            });

            userMenus.values().forEach(menuInventoryList -> {
                menuInventoryList.forEach(menuInventory -> {
                    Inventory inventory = menuInventory.getInventory();

                    Placeholder pagePlaceholder = new Placeholder("%page%", getInventoryIndex(inventory) + "");
                    modifiers.add(pagePlaceholder);

                    new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                        InventoryUpdate.updateInventory(JustAPIPlugin.getInstance(), (Player) viewer, Format.format(menuInventory.getTitle(),
                                modifiers.toArray(new StringModifier[]{})));
                    });
                });
            });
        }, 5, 1);
    }

    public void startUpdateTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            return;
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(JustAPIPlugin.getInstance(), () -> {
            if (activePlayers.isEmpty()) {
                updateTask.cancel();
                updateTask = null;
                return;
            }

            pushUpdates();
            pushUserUpdates();
        }, 0, 5);
    }

    private void pushUpdates() {
        if (data.getMaxInventories() != -1) {
            Inventory finalInventory = inventories.get(inventories.size() - 1).getInventory();

            if (!InventoryUtil.hasSpace(finalInventory, null, data.getValidSlots())) {
                generateNewInventory().ifPresent(inventories::add);
            }
        }

        for (MenuInventory menuInventory : inventories) {
            Inventory inventory = menuInventory.getInventory();
            Iterator<MenuItem> slotIterator = items.iterator();

            while (slotIterator.hasNext()) {
                MenuItem menuItem = slotIterator.next();

                if (!menuItem.getSourceInventories().contains(inventory)) {
                    menuItem.getSourceInventories().add(inventory);
                }

                if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                    menuItem.getSlots().forEach(slot -> inventory.setItem(slot, null));
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
                updateItemMeta(updatedItem);

                slots.forEach(slot -> inventory.setItem(slot, updatedItem));
            }
        }
    }

    private void pushUserUpdates() {
        userMenus.forEach((uuid, allInventories) -> {
            if (data.getMaxInventories() != -1) {
                Inventory finalInventory = allInventories.get(allInventories.size() - 1).getInventory();

                if (!InventoryUtil.hasSpace(finalInventory, null, data.getValidSlots())) {
                    generateNewInventory().ifPresent(allInventories::add);
                }
            }

            for (MenuInventory menuInventory : allInventories) {
                Inventory inventory = menuInventory.getInventory();
                List<Integer> validSlots = new ArrayList<>(data.getValidSlots());
                Iterator<MenuItem> slotIterator = items.iterator();

                while (slotIterator.hasNext()) {
                    MenuItem menuItem = slotIterator.next();

                    if (!menuItem.getSourceInventories().contains(inventory)) {
                        continue;
                    }

                    if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                        menuItem.getSlots().forEach(slot -> inventory.setItem(slot, null));
                        return;
                    }

                    List<Integer> slots = menuItem.getSlots();

                    if (menuItem.getItem() == null) {
                        slots.forEach(slot -> inventory.setItem(slot, null));
                        continue;
                    }

                    ItemStack item = menuItem.getItem().clone();
                    updateItemMeta(item);

                    if (slots.isEmpty()) {
                        int availableSlot = validSlots.get(0);
                        slots.add(availableSlot);
                        validSlots.remove(availableSlot);
                        menuItem.setSlots(slots);
                    }

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

    public void addItemStack(ItemStack item) {
        if (inventories.isEmpty()) {
            getInventory();
        }

        HashMap<MenuInventory, Integer> itemDistribution = new HashMap<>();

        ItemStack clonedItem = item.clone();
        int amountLeft = item.getAmount();

        for (MenuInventory menuInventory : inventories) {
            Inventory inventory = menuInventory.getInventory();
            int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, data.getValidSlots());

            if (availableSpace >= amountLeft) {
                amountLeft = 0;
                InventoryUtil.addItem(inventory, clonedItem, data.getValidSlots());
                break;
            }

            itemDistribution.put(menuInventory, availableSpace);
            amountLeft -= availableSpace;
            clonedItem.setAmount(amountLeft);
        }

        while (amountLeft > 0) {
            Optional<MenuInventory> generatedOptional = generateNewInventory();

            if (!generatedOptional.isPresent() || (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories())) {
                break;
            }

            MenuInventory menuInventory = generatedOptional.get();
            inventories.add(menuInventory);

            Inventory inventory = menuInventory.getInventory();
            int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, data.getValidSlots());

            if (availableSpace >= amountLeft) {
                amountLeft = 0;
                InventoryUtil.addItem(inventory, clonedItem, data.getValidSlots());
                break;
            }

            itemDistribution.put(menuInventory, availableSpace);
            amountLeft -= availableSpace;
            clonedItem.setAmount(amountLeft);
        }

        itemDistribution.forEach((menuInventory, amount) -> {
            Inventory inventory = menuInventory.getInventory();

            ItemStack distributionItem = item.clone();
            distributionItem.setAmount(amount);
            InventoryUtil.addItem(inventory, distributionItem, data.getValidSlots());
        });

        for (Map.Entry<UUID, List<MenuInventory>> entry : userMenus.entrySet()) {
            UUID player = entry.getKey();
            List<MenuInventory> userInventories = entry.getValue();

            itemDistribution.clear();
            clonedItem = item.clone();
            amountLeft = item.getAmount();

            for (MenuInventory menuInventory : userInventories) {
                Inventory inventory = menuInventory.getInventory();
                int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, data.getValidSlots());

                if (availableSpace >= amountLeft) {
                    amountLeft = 0;
                    InventoryUtil.addItem(inventory, clonedItem, data.getValidSlots());
                    break;
                }

                itemDistribution.put(menuInventory, availableSpace);
                amountLeft -= availableSpace;
                clonedItem.setAmount(amountLeft);
            }

            while (amountLeft > 0) {
                Optional<MenuInventory> generatedOptional = generateNewInventory();

                if (!generatedOptional.isPresent() || (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories())) {
                    break;
                }

                MenuInventory menuInventory = generatedOptional.get();
                userInventories.add(menuInventory);

                Inventory inventory = menuInventory.getInventory();
                int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, data.getValidSlots());

                if (availableSpace >= amountLeft) {
                    amountLeft = 0;
                    InventoryUtil.addItem(inventory, clonedItem, data.getValidSlots());
                    break;
                }

                itemDistribution.put(menuInventory, availableSpace);
                amountLeft -= availableSpace;
                clonedItem.setAmount(amountLeft);
            }

            itemDistribution.forEach((menuInventory, amount) -> {
                Inventory inventory = menuInventory.getInventory();

                ItemStack distributionItem = item.clone();
                distributionItem.setAmount(amount);
                InventoryUtil.addItem(inventory, distributionItem, data.getValidSlots());
            });

            userMenus.put(player, userInventories);
        }
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

    public List<MenuItem> getItems(Inventory inventory, int slot) {
        return items.stream().filter(item -> item.getSlots().contains(slot) && item.getSourceInventories().contains(inventory)).collect(Collectors.toList());
    }

    /**
     * Get a MenuItem instance by its slot.
     *
     * @param slot The MenuItem slot.
     * @return The optional MenuItem instance.
     */
    public Optional<MenuItem> getItem(Inventory inventory, int slot) {
        List<MenuItem> items = getItems(inventory, slot);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    private Optional<MenuInventory> generateNewInventory() {
        if (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories()) {
            return Optional.empty();
        }

        List<Integer> validSlots = new ArrayList<>(data.getValidSlots());

        if (validSlots.isEmpty()) {
            for (int i = 0; i < (items.size() > size - 1 ? size - 9 : size); i++) {
                validSlots.add(i);
            }
        }

        AtomicReference<List<Integer>> availableSlots = new AtomicReference<>(new ArrayList<>(validSlots));
        Inventory inventory = Bukkit.createInventory(this, size, ChatColor.RED.toString());

        List<MenuItem> itemCache = new ArrayList<>();

        getData().getItemFromStorage("Next-Page-Item").ifPresent(nextPageItem -> {
            nextPageItem.addSourceInventory(inventory);
            nextPageItem.addViewRequirement(new NextPageViewRequirement());
            nextPageItem.addAction(event -> {
                Inventory currentInventory = event.getClickedInventory();
                event.getWhoClicked().openInventory(getInventories().get(getInventoryIndex(currentInventory) + 1).getInventory());
            });
            nextPageItem.setSlots((List<Integer>) getData().getMetaData().getOrDefault("Next-Page-Slots", new ArrayList<>()));

            nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
            itemCache.add(nextPageItem);
        });

        getData().getItemFromStorage("Previous-Page-Item").ifPresent(previousPageItem -> {
            previousPageItem.addSourceInventory(inventory);
            previousPageItem.addViewRequirement(new PreviousPageViewRequirement());
            previousPageItem.addAction(event -> {
                Inventory currentInventory = event.getClickedInventory();
                event.getWhoClicked().openInventory(getInventories().get(getInventoryIndex(currentInventory) - 1).getInventory());
            });
            previousPageItem.setSlots((List<Integer>) getData().getMetaData().getOrDefault("Previous-Page-Slots", new ArrayList<>()));

            previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
            itemCache.add(previousPageItem);
        });

        items.forEach(menuItem -> {
            ItemStack item = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);
            updateItemMeta(item);

            if (!menuItem.getSlots().isEmpty()) {
                menuItem.addSourceInventory(inventory);
                menuItem.getSlots().forEach(slot -> {
                    availableSlots.get().removeAll(Collections.singletonList(slot));

                    if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                        return;
                    }

                    inventory.setItem(slot, item);
                });

                return;
            }

            int slot = availableSlots.get().get(0);
            availableSlots.get().removeAll(Collections.singletonList(slot));

            menuItem.addSourceInventory(inventory);
            menuItem.getSlots().add(slot);

            if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                return;
            }

            inventory.setItem(slot, item);
        });

        items.addAll(itemCache);

        if (data.getFillerItem() != null && data.getFillerItem().getItem().getType() != Material.AIR) {
            MenuItem fillerItem = data.getFillerItem();

            while (inventory.firstEmpty() != -1) {
                int slot = inventory.firstEmpty();
                inventory.setItem(slot, fillerItem.getItem());
            }
        }

        return Optional.of(new MenuInventory(inventory, title));
    }

    /**
     * Get the built Inventory.
     *
     * @return The build Inventory.
     */
    @Override
    public Inventory getInventory() {
        if (!inventories.isEmpty()) {
            return inventories.get(0).getInventory();
        }

        for(int i = 0; i < data.getMinInventories(); i++) {
            generateNewInventory().ifPresent(inventories::add);
        }

        return inventories.get(0).getInventory();
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
            userMenus.get(player.getUniqueId()).forEach(inv -> new ArrayList<>(inv.getInventory().getViewers()).forEach(HumanEntity::closeInventory));
            userMenus.remove(player.getUniqueId());
        }

        generateNewInventory().ifPresent(inventory -> userMenus.put(player.getUniqueId(), inventories));
        return userMenus.get(player.getUniqueId()).get(0).getInventory();
    }

    public Inventory getGlobalMenu() {
        if (inventories.isEmpty()) {
            getInventory();
        }

        return inventories.get(0).getInventory();
    }

    public MenuInventory getMenuInventory(Inventory inventory) {
        return inventories.stream().filter(mInventory -> mInventory.getInventory().equals(inventory)).findFirst().orElse(null);
    }

    public int getInventoryIndex(Inventory inventory) {
        MenuInventory menuInventory = getMenuInventory(inventory);

        if (menuInventory == null) {
            return -1;
        }

        return inventories.indexOf(menuInventory);
    }

    public long getTotalCapacity() {
        if (data.getMaxInventories() == -1) {
            return -1;
        }

        return data.getValidSlots().size() * 64L * data.getMaxInventories();
    }

    public long getCurrentCapacity() {
        long currentCapacity = 0;

        for (MenuInventory menuInventory : inventories) {
            Inventory inventory = menuInventory.getInventory();

            for (Integer validSlot : data.getValidSlots()) {
                if (inventory.getItem(validSlot) == null || inventory.getItem(validSlot).getType() == Material.AIR) {
                    continue;
                }

                currentCapacity += inventory.getItem(validSlot).getAmount();
            }
        }

        return currentCapacity;
    }

    @Override
    public Menu clone() {
        List<MenuItem> items = new ArrayList<>();
        this.items.forEach(item -> items.add(item.clone()));
        return new Menu(id, title, size, items, data.clone(), new ArrayList<>(), new HashMap<>());
    }

}
