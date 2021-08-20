package net.justugh.japi.menu;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.menu.placeholder.PlaceholderProvider;

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
    private final Map<String, MenuAction> actions;
    private final Map<String, Object> metaData;
    private final List<PlaceholderProvider> placeholderProviders;

    public MenuData() {
        this.itemStorage = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.actions = new HashMap<>();
        this.metaData = new HashMap<>();
        this.placeholderProviders = new ArrayList<>();
    }

    public MenuItem getItemFromStorage(String identifier) {
        return itemStorage.stream().filter(item -> item.getIdentifier() != null && item.getIdentifier().equalsIgnoreCase(identifier)).findFirst().orElse(null);
    }

    public void registerAction(String identifier, MenuAction action) {
        actions.put(identifier, action);
    }

    public MenuAction getAction(String identifier) {
        return actions.get(identifier);
    }

    public void addFlag(MenuFlag flag) {
        flags.add(flag);
    }

    public boolean hasFlag(MenuFlag flag) {
        return flags.contains(flag);
    }

}
