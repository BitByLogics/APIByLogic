package net.bitbylogic.apibylogic.menu.item;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuAction;
import net.bitbylogic.apibylogic.menu.action.MenuClickActionType;
import net.bitbylogic.apibylogic.menu.action.MenuClickRequirement;
import net.bitbylogic.apibylogic.menu.view.MenuViewRequirement;
import net.bitbylogic.apibylogic.util.GenericHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Getter
public class MenuItem implements Cloneable {

    private static final MenuItemConfigParser CONFIG_PARSER = new MenuItemConfigParser();

    private final @NotNull String id;

    private final @NonNull List<Integer> slots;
    private final @NotNull List<Inventory> sourceInventories;

    private final @NotNull List<MenuAction> actions;
    private final @NotNull HashMap<MenuClickActionType, String> internalActions;

    private final @NotNull List<MenuClickRequirement> clickRequirements;
    private final @NotNull List<MenuViewRequirement> viewRequirements;

    private final @NonNull GenericHashMap<String, Object> metadata;

    private @Nullable ItemStack item;
    private @Nullable MenuItemUpdateProvider itemUpdateProvider;

    @Setter
    private @Nullable Menu menu;

    private boolean filler;
    private boolean updatable;
    @Setter
    private boolean locked;
    private boolean saved = true;

    public MenuItem(@NonNull String id) {
        this.id = id;

        this.slots = new ArrayList<>();
        this.sourceInventories = new ArrayList<>();

        this.actions = new ArrayList<>();
        this.internalActions = new HashMap<>();

        this.clickRequirements = new ArrayList<>();
        this.viewRequirements = new ArrayList<>();

        this.metadata = new GenericHashMap<>();
    }

    public MenuItem(@NotNull String id, @NonNull List<Integer> slots, @NotNull List<Inventory> sourceInventories,
                    @NotNull List<MenuAction> actions, @NotNull HashMap<MenuClickActionType, String> internalActions,
                    @NotNull List<MenuClickRequirement> clickRequirements, @NotNull List<MenuViewRequirement> viewRequirements,
                    @NonNull GenericHashMap<String, Object> metadata, @Nullable ItemStack item,
                    @Nullable MenuItemUpdateProvider itemUpdateProvider, boolean filler, boolean updatable, boolean saved) {
        this.id = id;
        this.slots = slots;
        this.sourceInventories = sourceInventories;
        this.actions = actions;
        this.internalActions = internalActions;
        this.clickRequirements = clickRequirements;
        this.viewRequirements = viewRequirements;
        this.metadata = metadata;
        this.item = item;
        this.itemUpdateProvider = itemUpdateProvider;
        this.filler = filler;
        this.updatable = updatable;
        this.saved = saved;
        this.locked = false;
    }

    public MenuItem withSlot(int slot) {
        if(locked) {
            return this;
        }

        this.slots.add(slot);
        return this;
    }

    public MenuItem withSlots(@NonNull List<Integer> slots) {
        if(locked) {
            return this;
        }

        this.slots.addAll(slots);
        return this;
    }

    public MenuItem withSourceInventory(@NonNull Inventory inventory) {
        this.sourceInventories.add(inventory);
        return this;
    }

    public MenuItem withSourceInventories(@NonNull List<Inventory> sourceInventories) {
        this.sourceInventories.addAll(sourceInventories);
        return this;
    }

    public MenuItem withAction(@NonNull MenuAction action) {
        actions.add(action);
        return this;
    }

    public MenuItem withActions(@NonNull List<MenuAction> actions) {
        this.actions.addAll(actions);
        return this;
    }

    public MenuItem withInternalAction(@NonNull MenuClickActionType actionType, @NonNull String data) {
        if(locked) {
            return this;
        }

        this.internalActions.put(actionType, data);
        return this;
    }

    public MenuItem withInternalActions(@NonNull HashMap<MenuClickActionType, String> internalActions) {
        if(locked) {
            return this;
        }

        this.internalActions.putAll(internalActions);
        return this;
    }

    public MenuItem withClickRequirement(@NonNull MenuClickRequirement requirement) {
        this.clickRequirements.add(requirement);
        return this;
    }

    public MenuItem withClickRequirement(@NonNull List<MenuClickRequirement> clickRequirements) {
        this.clickRequirements.addAll(clickRequirements);
        return this;
    }

    public MenuItem withViewRequirement(@NonNull MenuViewRequirement requirement) {
        this.viewRequirements.add(requirement);
        return this;
    }

    public MenuItem withViewRequirements(@NonNull List<MenuViewRequirement> viewRequirements) {
        this.viewRequirements.addAll(viewRequirements);
        return this;
    }

    public MenuItem withMetadata(@NonNull String key, @NonNull Object value) {
        if(locked) {
            return this;
        }

        metadata.put(key, value);
        return this;
    }

    public MenuItem withMetadata(@NonNull GenericHashMap<String, Object> metadata) {
        if(locked) {
            return this;
        }

        this.metadata.putAll(metadata);
        return this;
    }

    public MenuItem item(@Nullable ItemStack item) {
        if(locked) {
            return this;
        }

        this.item = item;
        return this;
    }

    public MenuItem filler(boolean filler) {
        if(locked) {
            return this;
        }

        this.filler = filler;
        return this;
    }

    public MenuItem updatable(boolean updatable) {
        if(locked) {
            return this;
        }

        this.updatable = updatable;
        return this;
    }

    public MenuItem saved(boolean saved) {
        this.saved = saved;
        return this;
    }

    public MenuItem updateProvider(@NonNull MenuItemUpdateProvider updateProvider) {
        this.itemUpdateProvider = updateProvider;
        return this;
    }

    public void onClick(@NonNull InventoryClickEvent event) {
        if (clickRequirements.stream().anyMatch(requirement -> !requirement.canClick((Player) event.getWhoClicked()))) {
            return;
        }

        internalActions.keySet().forEach(action -> action.getAction().onClick(event, internalActions.get(action)));
        actions.forEach(action -> action.onClick(event));
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section) {
        return saveToConfig(section, false);
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section, boolean overwrite) {
        if(!overwrite && section.isSet(id) || !saved) {
            return false;
        }

        ConfigurationSection itemSection = section.getConfigurationSection(id);

        if(itemSection == null) {
            itemSection = section.createSection(id);
        }

        CONFIG_PARSER.parseTo(itemSection, this);
        return true;
    }

    public static Optional<MenuItem> getFromConfig(@Nullable ConfigurationSection section) {
        if(section == null) {
            return Optional.empty();
        }

        return CONFIG_PARSER.parseFrom(section);
    }

    public MenuItem clone() {
        return clone(true);
    }

    public MenuItem clone(boolean cloneAction) {
        GenericHashMap<String, Object> metadata = new GenericHashMap<>();
        metadata.putAll(this.metadata);

        return new MenuItem(
                id, new ArrayList<>(slots), new ArrayList<>(sourceInventories),
                cloneAction ? new ArrayList<>(actions) : new ArrayList<>(),
                new HashMap<>(internalActions), new ArrayList<>(clickRequirements),
                new ArrayList<>(viewRequirements), metadata, item, itemUpdateProvider,
                filler, updatable, false
        );
    }
}
