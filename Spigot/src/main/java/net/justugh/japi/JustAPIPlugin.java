package net.justugh.japi;

import lombok.Getter;
import net.justugh.japi.action.ActionManager;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.listener.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@Getter
public class JustAPIPlugin extends JavaPlugin {

    @Getter
    private static JustAPIPlugin instance;
    private final List<Menu> activeMenus = new ArrayList<>();

    private RedisManager redisManager;
    private ActionManager actionManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        redisManager = new RedisManager(
                getConfig().getString("Redis-Credentials.Host"),
                getConfig().getInt("Redis-Credentials.Port"),
                getConfig().getString("Redis-Credentials.Password"), getConfig().getString("Server-ID"));
        actionManager = new ActionManager(this);

        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);

        startMenuTask();
    }

    @Override
    public void onDisable() {

    }

    private void startMenuTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            getActiveMenus().forEach(menu -> {
                Iterator<Map.Entry<UUID, List<Inventory>>> entryIterator = menu.getUserMenus().entrySet().iterator();

                while (entryIterator.hasNext()) {
                    Map.Entry<UUID, List<Inventory>> entry = entryIterator.next();

                    if (entry.getValue().stream().anyMatch(inventory -> inventory.getViewers().isEmpty())) {
                        continue;
                    }

                    entryIterator.remove();
                }
            });
        }, 0, 200);
    }

}
