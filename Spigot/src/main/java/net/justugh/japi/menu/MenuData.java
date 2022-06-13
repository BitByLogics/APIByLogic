package net.justugh.japi.menu;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.menu.placeholder.PlaceholderProvider;
import net.justugh.japi.menu.placeholder.UserPlaceholderProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MenuData {

    private final List<MenuItem> itemStorage;

    private MenuItem fillerItem;
    private MenuAction externalClickAction;
    private MenuCloseAction closeAction;

    private final List<MenuFlag> flags;
    private final Map<String, Object> metaData;
    private final List<Integer> validSlots;
    private final List<PlaceholderProvider> placeholderProviders;
    private final List<UserPlaceholderProvider> userPlaceholderProviders;

    public MenuData() {
        this.itemStorage = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.metaData = new HashMap<>();
        this.validSlots = new ArrayList<>();
        this.placeholderProviders = new ArrayList<>();
        this.userPlaceholderProviders = new ArrayList<>();
    }

    public MenuItem getItemFromStorage(String identifier) {
        return itemStorage.stream().filter(item -> item.getIdentifier() != null && item.getIdentifier().equalsIgnoreCase(identifier)).findFirst().orElse(null);
    }

    public void addFlag(MenuFlag flag) {
        flags.add(flag);
    }

    public boolean hasFlag(MenuFlag flag) {
        return flags.contains(flag);
    }

}
