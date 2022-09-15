package net.justugh.japi;

import lombok.Getter;
import net.justugh.japi.action.ActionManager;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.menu.manager.MenuManager;
import net.justugh.japi.util.event.armor.listener.ArmorListener;
import net.justugh.japi.util.event.armor.listener.DispenserArmorListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class JustAPIPlugin extends JavaPlugin {

    @Getter
    private static JustAPIPlugin instance;

    private RedisManager redisManager;
    private ActionManager actionManager;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!getConfig().getString("Redis-Credentials.Host").isEmpty()) {
            redisManager = new RedisManager(
                    getConfig().getString("Redis-Credentials.Host"),
                    getConfig().getInt("Redis-Credentials.Port"),
                    getConfig().getString("Redis-Credentials.Password"), getConfig().getString("Server-ID"));
        }

        actionManager = new ActionManager(this);
        menuManager = new MenuManager(this);

        Bukkit.getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("Ignored-Materials")), this);
        Bukkit.getPluginManager().registerEvents(new DispenserArmorListener(), this);
    }

    @Override
    public void onDisable() {

    }

}
