package net.justugh.japi.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.menu.placeholder.PlaceholderProvider;
import net.justugh.japi.menu.placeholder.UserPlaceholderProvider;
import net.justugh.japi.util.StringModifier;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
public class MenuData implements Cloneable {

    private final List<MenuItem> itemStorage;

    private MenuItem fillerItem;
    private MenuCloseAction closeAction;
    private int maxInventories = -1;

    private final List<MenuFlag> flags;
    private final Map<String, Object> metaData;
    private final List<Integer> validSlots;
    private final List<PlaceholderProvider> placeholderProviders;
    private final List<UserPlaceholderProvider> userPlaceholderProviders;
    private final List<StringModifier> modifiers;

    public MenuData() {
        this.itemStorage = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.metaData = new HashMap<>();
        this.validSlots = new ArrayList<>();
        this.placeholderProviders = new ArrayList<>();
        this.userPlaceholderProviders = new ArrayList<>();
        this.modifiers = new ArrayList<>();
    }

    public Optional<MenuItem> getItemFromStorage(String identifier) {
        return itemStorage.stream().filter(item -> item.getIdentifier() != null && item.getIdentifier().equalsIgnoreCase(identifier)).findFirst();
    }

    public void addFlag(MenuFlag flag) {
        flags.add(flag);
    }

    public boolean hasFlag(MenuFlag flag) {
        return flags.contains(flag);
    }

    public void addModifier(StringModifier modifier) {
        modifiers.add(modifier);
    }

    @Override
    public MenuData clone() {
        List<MenuItem> itemStorage = new ArrayList<>();
        this.itemStorage.forEach(item -> itemStorage.add(item.clone()));
        return new MenuData(itemStorage, fillerItem, closeAction, maxInventories,
                flags, metaData, validSlots, placeholderProviders,
                userPlaceholderProviders, modifiers);
    }
}
