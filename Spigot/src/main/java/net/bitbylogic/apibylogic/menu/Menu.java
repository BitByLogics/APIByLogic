package net.bitbylogic.apibylogic.menu;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.inventory.MenuInventory;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import net.bitbylogic.apibylogic.menu.task.MenuUpdateTask;
import net.bitbylogic.apibylogic.menu.task.TitleUpdateTask;
import net.bitbylogic.apibylogic.menu.view.internal.NextPageViewRequirement;
import net.bitbylogic.apibylogic.menu.view.internal.PreviousPageViewRequirement;
import net.bitbylogic.apibylogic.util.Pair;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.PlaceholderProvider;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.inventory.InventoryUtil;
import net.bitbylogic.apibylogic.util.item.ItemStackUtil;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
@Setter
public class Menu implements InventoryHolder, Cloneable {

    private static final MenuConfigParser CONFIG_PARSER = new MenuConfigParser();

    private final String id;
    private final String title;
    private final int size;

    private final MenuData data;

    private final List<MenuItem> items;
    private final List<UUID> viewers;

    @Getter(AccessLevel.NONE)
    private final List<MenuInventory> inventories;

    private final MenuUpdateTask updateTask;
    private final TitleUpdateTask titleUpdateTask;

    public Menu(@NonNull String id, @NonNull String title, @NonNull MenuRows menuRows) {
        this(id, title, menuRows.getSize());
    }

    public Menu(@NonNull String id, @NonNull String title, int size) {
        this(id, title, size, null);
    }

    public Menu(@NonNull String id, @NonNull String title, int size, @Nullable MenuData data) {
        this.id = id;
        this.title = title;
        this.size = size;

        this.items = new ArrayList<>();
        this.data = data == null ? new MenuData() : data;
        this.inventories = new ArrayList<>();
        this.viewers = new ArrayList<>();

        titleUpdateTask = new TitleUpdateTask(this);
        updateTask = new MenuUpdateTask(this);
    }

    public Menu(@NonNull String id, @NonNull String title, int size, @Nullable MenuData data,
                @Nullable List<MenuItem> items, @Nullable List<MenuInventory> inventories) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.data = data == null ? new MenuData() : data;
        this.items = items == null ? new ArrayList<>() : items;
        this.inventories = inventories == null ? new ArrayList<>() : inventories;
        this.viewers = new ArrayList<>();

        titleUpdateTask = new TitleUpdateTask(this);
        updateTask = new MenuUpdateTask(this);
    }

    public static Optional<Menu> getFromConfig(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }

        return CONFIG_PARSER.parseFrom(section);
    }

    /**
     * Add an item to the Menu.
     *
     * @param item The item to add.
     * @return The Menu instance.
     */
    public Menu addItem(MenuItem item) {
        item.setMenu(this);
        items.add(item);
        return this;
    }

    /**
     * Add an item to the Menu and set its
     * slot to the next available slot
     *
     * @param item The item to add.
     */
    public void addAndSetItem(@NonNull MenuItem item) {
        if (inventories.isEmpty()) {
            generateInventories();
        }

        item.setMenu(this);
        items.add(item);

        Pair<Inventory, Integer> availableSlot = getNextAvailableSlot();

        if (availableSlot == null) {
            return;
        }

        boolean locked = item.isLocked();

        item.getSlots().clear();

        if (locked) {
            item.setLocked(false);
        }

        item.withSlot(availableSlot.getValue());
        item.withSourceInventory(availableSlot.getKey());
        item.setLocked(locked);
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

            if (generatedOptional.isEmpty() || (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories())) {
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
        item.setMenu(this);
        item.withSlot(slot);
        items.add(item);
        return this;
    }

    /**
     * Get a MenuItem instance by its id.
     *
     * @param id The MenuItem identifier.
     * @return The optional MenuItem instance.
     */
    public Optional<MenuItem> getItem(String id) {
        return items.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();
    }

    public MenuItem getItemOrCreate(@NonNull String id) {
        Optional<MenuItem> foundItem = getItem(id);

        if (foundItem.isPresent()) {
            return foundItem.get();
        }

        MenuItem fallbackItem = new MenuItem(id);
        fallbackItem.setMenu(this);

        items.add(fallbackItem);
        return fallbackItem;
    }

    public Menu withItem(@NonNull MenuItem menuItem) {
        Optional<MenuItem> foundItem = getItem(menuItem.getId());

        if (foundItem.isPresent()) {
            return this;
        }

        if (menuItem.getSlots().isEmpty()) {
            if (data.getStoredItem(menuItem.getId()).isPresent()) {
                return this;
            }

            data.getItemStorage().add(menuItem);
            return this;
        }

        addItem(menuItem);
        return this;
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
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
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
        modifiers.addAll(data.getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).toList());

        Placeholder pagesPlaceholder = new Placeholder("%pages%", inventories.size() + 1 + "");
        Placeholder pagePlaceholder = new Placeholder("%page%", inventories.size() + 1 + "");
        modifiers.add(pagesPlaceholder);
        modifiers.add(pagePlaceholder);

        AtomicReference<List<Integer>> availableSlots = new AtomicReference<>(new ArrayList<>(validSlots));
        Inventory inventory = Bukkit.createInventory(this, size, Formatter.format(title, modifiers.toArray(new StringModifier[]{})));

        List<MenuItem> itemCache = new ArrayList<>();

        getData().getStoredItem("Next-Page-Item").ifPresent(nextPageItem -> {
            nextPageItem.setMenu(this);
            nextPageItem.withSourceInventory(inventory);

            if (!data.hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
                nextPageItem.withViewRequirement(new NextPageViewRequirement());
            }

            nextPageItem.withAction(event -> {
                Inventory currentInventory = event.getClickedInventory();
                int nextIndex = getInventoryIndex(currentInventory) + 1;

                if (nextIndex > getInventories().size() - 1) {
                    return;
                }

                event.getWhoClicked().openInventory(getInventories().get(nextIndex).getInventory());
            });

            nextPageItem.withSlots(getData().getMetadata().getValueAsOrDefault("Next-Page-Slots", new ArrayList<>()));
            nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));

            itemCache.add(nextPageItem);
        });

        getData().getStoredItem("Previous-Page-Item").ifPresent(previousPageItem -> {
            previousPageItem.setMenu(this);
            previousPageItem.withSourceInventory(inventory);

            if (!data.hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
                previousPageItem.withViewRequirement(new PreviousPageViewRequirement());
            }

            previousPageItem.withAction(event -> {
                Inventory currentInventory = event.getClickedInventory();
                int previousIndex = getInventoryIndex(currentInventory) - 1;

                if (previousIndex <= -1) {
                    return;
                }

                event.getWhoClicked().openInventory(getInventories().get(previousIndex).getInventory());
            });

            previousPageItem.withSlots(getData().getMetadata().getValueAsOrDefault("Previous-Page-Slots", new ArrayList<>()));
            previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));

            itemCache.add(previousPageItem);
        });

        items.forEach(menuItem -> {
            if (menuItem.getItem() == null && menuItem.getItemUpdateProvider() == null) {
                return;
            }

            ItemStack item = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);

            if (!data.getModifiers().isEmpty() || !data.getPlaceholderProviders().isEmpty()) {
                List<StringModifier> placeholders = new ArrayList<>(data.getModifiers());
                data.getPlaceholderProviders().forEach(placeholder -> placeholders.add(placeholder.asPlaceholder()));

                ItemStackUtil.updateItem(item, placeholders.toArray(new StringModifier[]{}));
            }

            if (!menuItem.getSlots().isEmpty()) {
                menuItem.withSourceInventory(inventory);
                menuItem.getSlots().forEach(slot -> {
                    availableSlots.get().removeAll(Collections.singletonList(slot));

                    if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                        return;
                    }

                    inventory.setItem(slot, item);
                });

                return;
            }

            int slot = availableSlots.get().getFirst();
            availableSlots.get().removeAll(Collections.singletonList(slot));

            menuItem.withSourceInventory(inventory);
            menuItem.getSlots().add(slot);

            if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                return;
            }

            inventory.setItem(slot, item);
        });

        items.addAll(itemCache);

        data.getFillerItem().ifPresent(fillerItem -> {
            if (fillerItem.getItem() == null || fillerItem.getItem().getType().isAir()) {
                return;
            }

            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) != null || getData().getValidSlots().contains(i)) {
                    continue;
                }

                inventory.setItem(i, fillerItem.getItem());
            }
        });

        return Optional.of(new MenuInventory(inventory, title));
    }

    /**
     * Get the built Inventory.
     *
     * @return The build Inventory.
     */
    @Override
    public @NotNull Inventory getInventory() {
        if (inventories.isEmpty()) {
            generateInventories();
        }

        return inventories.getFirst().getInventory();
    }

    public void open(@NonNull Player player, int page) {
        if (inventories.isEmpty()) {
            generateInventories();
        }

        if (page > inventories.size()) {
            return;
        }

        MenuInventory inventory = inventories.get(page - 1);
        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), () -> player.openInventory(inventory.getInventory()), 1);
    }

    public void open(@NonNull Player player) {
        open(player, 1);
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

        return inventories.getFirst().getInventory();
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
                ItemStack slotItem = inventory.getItem(validSlot);

                if (slotItem == null || slotItem.getType().isAir()) {
                    continue;
                }

                currentCapacity += slotItem.getAmount();
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

    public boolean saveToConfig(@NonNull ConfigurationSection section) {
        return saveToConfig(section, false);
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section, boolean overwrite) {
        ConfigurationSection itemsSection = section.getConfigurationSection(id + ".Items");

        if (itemsSection != null) {
            items.stream().filter(MenuItem::isSaved).forEach(menuItem -> menuItem.saveToConfig(itemsSection, overwrite));
            data.getItemStorage().stream().filter(MenuItem::isSaved).forEach(menuItem -> menuItem.saveToConfig(itemsSection, overwrite));
        }

        if (section.isSet(id) && !overwrite) {
            return false;
        }

        CONFIG_PARSER.parseTo(section.createSection(id), this);
        return true;
    }

    @Override
    public Menu clone() {
        List<MenuItem> items = new ArrayList<>();
        this.items.forEach(item -> items.add(item.clone()));
        return new Menu(id, title, size, data.clone(), items, new ArrayList<>());
    }

}
