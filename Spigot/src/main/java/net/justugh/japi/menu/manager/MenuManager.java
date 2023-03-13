package net.justugh.japi.menu.manager;

import lombok.Getter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuAction;
import net.justugh.japi.menu.action.MenuClickRequirement;
import net.justugh.japi.menu.inventory.MenuInventory;
import net.justugh.japi.menu.listener.MenuListener;
import net.justugh.japi.util.StringModifier;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.*;

@Getter
public class MenuManager {

    private final JustAPIPlugin plugin;

    private final HashMap<String, MenuAction> actions = new HashMap<>();
    private final HashMap<String, MenuClickRequirement> requirements = new HashMap<>();
    private final List<StringModifier> stringModifiers = new ArrayList<>();

    private final List<Menu> activeMenus = new ArrayList<>();

    public MenuManager(JustAPIPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(new MenuListener(), plugin);
        startMenuTask();
    }

    public void addGlobalAction(String identifier, MenuAction action) {
        actions.put(identifier, action);
    }

    public void addGlobalRequirement(String identifier, MenuClickRequirement requirement) {
        requirements.put(identifier, requirement);
    }

    public void addGlobalStringModifier(StringModifier modifier) {
        stringModifiers.add(modifier);
    }

    public MenuAction getGlobalAction(String identifier) {
        for (String id : actions.keySet()) {
            if (!id.equalsIgnoreCase(identifier)) {
                continue;
            }

            return actions.get(id);
        }

        return null;
    }

    public MenuClickRequirement getGlobalRequirement(String identifier) {
        for (String id : requirements.keySet()) {
            if (!id.equalsIgnoreCase(identifier)) {
                continue;
            }

            return requirements.get(id);
        }

        return null;
    }

    private void startMenuTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeMenus.forEach(menu -> {
                Iterator<Map.Entry<UUID, List<MenuInventory>>> entryIterator = menu.getUserMenus().entrySet().iterator();

                while (entryIterator.hasNext()) {
                    Map.Entry<UUID, List<MenuInventory>> entry = entryIterator.next();

                    if (entry.getValue().stream().anyMatch(inventory -> inventory.getInventory().getViewers().isEmpty())) {
                        continue;
                    }

                    menu.getUpdateTask().cancel();
                    menu.setUpdateTask(null);
                    entryIterator.remove();
                }
            });
        }, 0, 200);
    }

}
