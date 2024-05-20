package net.bitbylogic.apibylogic.menu;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.menu.inventory.MenuInventory;
import net.bitbylogic.apibylogic.menu.placeholder.PlaceholderProvider;
import net.bitbylogic.apibylogic.menu.task.MenuUpdateTask;
import net.bitbylogic.apibylogic.menu.task.TitleUpdateTask;
import net.bitbylogic.apibylogic.menu.view.internal.NextPageViewRequirement;
import net.bitbylogic.apibylogic.menu.view.internal.PreviousPageViewRequirement;
import net.bitbylogic.apibylogic.util.*;
import net.bitbylogic.apibylogic.util.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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

    @Getter(AccessLevel.NONE)
    private final List<MenuInventory> inventories;
    private final List<UUID> activePlayers;

    private MenuUpdateTask updateTask;
    private TitleUpdateTask titleUpdateTask;
    private long lastUpdateCheck;

    public Menu(String title, Rows rows) {
        this(title, rows.getSize());
    }

    public Menu(String title, int size) {
        this(title, size, new MenuData());
    }

    public Menu(String title, int size, MenuData data) {
        this.title = title;
        this.size = size;

        this.items = new ArrayList<>();
        this.data = data;
        this.inventories = new ArrayList<>();
        this.activePlayers = new ArrayList<>();

        if (!data.hasFlag(MenuFlag.DISABLE_TITLE_UPDATE)) {
            titleUpdateTask = new TitleUpdateTask(this);
        }

        updateTask = new MenuUpdateTask(this);
    }

    public Menu(String id, String title, int size, List<MenuItem> items, MenuData data, List<MenuInventory> inventories, HashMap<UUID, List<MenuInventory>> userMenus) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.items = items;
        this.data = data;
        this.inventories = inventories;
        this.activePlayers = new ArrayList<>();

        if (!data.hasFlag(MenuFlag.DISABLE_TITLE_UPDATE)) {
            titleUpdateTask = new TitleUpdateTask(this);
        }

        updateTask = new MenuUpdateTask(this);
    }

    public void updateItemMeta(ItemStack item) {
        if (data.getModifiers().isEmpty() && data.getPlaceholderProviders().isEmpty()) {
            return;
        }

        List<StringModifier> placeholders = new ArrayList<>();

        placeholders.addAll(data.getModifiers());
        data.getPlaceholderProviders().forEach(placeholder -> placeholders.add(placeholder.asPlaceholder()));

        ItemStackUtil.updateItem(item, placeholders.toArray(new StringModifier[]{}));
    }

    public void updateUserItemMeta(MenuItem menuItem, OfflinePlayer player, ItemStack item) {
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
     * Add an item to the Menu and set its
     * slot to the next available slot
     *
     * @param item The item to add.
     * @return The Menu instance.
     */
    public Menu addAndSetItem(MenuItem item) {
        if (inventories.isEmpty()) {
            generateInventories();
        }

        items.add(item);

        Pair<Inventory, Integer> availableSlot = getNextAvailableSlot();

        if (availableSlot == null) {
            return this;
        }

        item.getSlots().clear();
        item.addSlot(availableSlot.getValue());
        item.addSourceInventory(availableSlot.getKey());
        return this;
    }

    public void addItemStack(ItemStack item) {
        if (inventories.isEmpty()) {
            generateInventories();
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

    public Optional<MenuInventory> generateNewInventory() {
        if (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories()) {
            return Optional.empty();
        }

        List<Integer> validSlots = new ArrayList<>(data.getValidSlots());

        if (validSlots.isEmpty()) {
            for (int i = 0; i < (items.size() > size - 1 ? size - 9 : size); i++) {
                validSlots.add(i);
            }
        }

        List<StringModifier> modifiers = new ArrayList<>();
        modifiers.addAll(data.getModifiers());
        modifiers.addAll(data.getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).collect(Collectors.toList()));

        Placeholder pagesPlaceholder = new Placeholder("%pages%", inventories.size() + 1 + "");
        Placeholder pagePlaceholder = new Placeholder("%page%", inventories.size() + 1 + "");
        modifiers.add(pagesPlaceholder);
        modifiers.add(pagePlaceholder);

        AtomicReference<List<Integer>> availableSlots = new AtomicReference<>(new ArrayList<>(validSlots));
        Inventory inventory = Bukkit.createInventory(this, size, data.hasFlag(MenuFlag.DISABLE_TITLE_UPDATE) ? Messages.format(title, modifiers.toArray(new StringModifier[]{})) : ChatColor.RED.toString());

        List<MenuItem> itemCache = new ArrayList<>();

        getData().getItemFromStorage("Next-Page-Item").ifPresent(nextPageItem -> {
            nextPageItem.addSourceInventory(inventory);

            if (!data.hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
                nextPageItem.addViewRequirement(new NextPageViewRequirement());
            }

            nextPageItem.addAction(event -> {
                Inventory currentInventory = event.getClickedInventory();
                int nextIndex = getInventoryIndex(currentInventory) + 1;

                if (nextIndex > getInventories().size() - 1) {
                    return;
                }

                event.getWhoClicked().openInventory(getInventories().get(nextIndex).getInventory());
            });
            nextPageItem.setSlots((List<Integer>) getData().getMetaData().getOrDefault("Next-Page-Slots", new ArrayList<>()));

            nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
            itemCache.add(nextPageItem);
        });

        getData().getItemFromStorage("Previous-Page-Item").ifPresent(previousPageItem -> {
            previousPageItem.addSourceInventory(inventory);

            if (!data.hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
                previousPageItem.addViewRequirement(new PreviousPageViewRequirement());
            }

            previousPageItem.addAction(event -> {
                Inventory currentInventory = event.getClickedInventory();
                int previousIndex = getInventoryIndex(currentInventory) - 1;

                if (previousIndex <= -1) {
                    return;
                }

                event.getWhoClicked().openInventory(getInventories().get(previousIndex).getInventory());
            });
            previousPageItem.setSlots((List<Integer>) getData().getMetaData().getOrDefault("Previous-Page-Slots", new ArrayList<>()));

            previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
            itemCache.add(previousPageItem);
        });

        //TODO: Implement a check to prevent "global" menu items
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

            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) != null || getData().getValidSlots().contains(i)) {
                    continue;
                }

                inventory.setItem(i, fillerItem.getItem());
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
        if (inventories.isEmpty()) {
            generateInventories();
        }

        return inventories.get(0).getInventory();
    }

    public List<MenuInventory> getInventories() {
        if (inventories.isEmpty()) {
            generateInventories();
        }

        return inventories;
    }

    private void generateInventories() {
        for (int i = 0; i < data.getMinInventories(); i++) {
            generateNewInventory().ifPresent(inventories::add);
        }
    }

    public Inventory getGlobalMenu() {
        if (inventories.isEmpty()) {
            generateInventories();
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

    public Pair<Inventory, Integer> getNextAvailableSlot() {
        if (inventories.isEmpty()) {
            generateInventories();
        }

        for (MenuInventory inventory : inventories) {
            if (data.getValidSlots().stream().noneMatch(slot -> inventory.getInventory().getItem(slot) == null)) {
                continue;
            }

            for (int validSlot : data.getValidSlots()) {
                if (inventory.getInventory().getItem(validSlot) != null) {
                    continue;
                }

                if (getItem(inventory.getInventory(), validSlot).isPresent()) {
                    continue;
                }

                return new Pair<>(inventory.getInventory(), validSlot);
            }
        }

        return null;
    }

    public HashMap<Inventory, HashMap<Integer, ItemStack>> getVanillaItems() {
        if (inventories == null || inventories.isEmpty()) {
            return new HashMap<>();
        }

        HashMap<Inventory, HashMap<Integer, ItemStack>> vanillaItems = new HashMap<>();

        for (MenuInventory menuInventory : inventories) {
            Inventory inventory = menuInventory.getInventory();
            HashMap<Integer, ItemStack> itemMap = vanillaItems.getOrDefault(inventory, new HashMap<>());


            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);

                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }

                if (getItem(inventory, slot).isPresent()) {
                    continue;
                }

                itemMap.put(slot, item);
            }

            if (itemMap.isEmpty()) {
                continue;
            }

            vanillaItems.put(inventory, itemMap);
        }

        return vanillaItems;
    }

    @Override
    public Menu clone() {
        List<MenuItem> items = new ArrayList<>();
        this.items.forEach(item -> items.add(item.clone()));
        return new Menu(id, title, size, items, data.clone(), new ArrayList<>(), new HashMap<>());
    }

}
