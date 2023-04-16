package net.justugh.japi;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.action.ActionManager;
import net.justugh.japi.command.JustAPICommand;
import net.justugh.japi.database.hikari.HikariAPI;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.listener.SpawnerListener;
import net.justugh.japi.menu.manager.MenuManager;
import net.justugh.japi.scoreboard.JustBoard;
import net.justugh.japi.util.ItemStackUtil;
import net.justugh.japi.util.event.armor.listener.ArmorListener;
import net.justugh.japi.util.event.armor.listener.DispenserArmorListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class JustAPIPlugin extends JavaPlugin {

    @Getter
    private static JustAPIPlugin instance;

    @Setter
    private boolean debug = false;

    private RedisManager redisManager;
    private HikariAPI hikariAPI;

    private ActionManager actionManager;
    private MenuManager menuManager;

    private List<JustBoard> activeBoards = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        ItemStackUtil.initialize();
        getCommand("justapi").setExecutor(new JustAPICommand());

        if (!getConfig().getString("Redis-Credentials.Host", "").isEmpty()) {
            ConfigurationSection redisSection = getConfig().getConfigurationSection("Redis-Credentials");

            CompletableFuture.supplyAsync(() -> new RedisManager(
                            redisSection.getString("Host"), redisSection.getInt("Port"),
                            redisSection.getString("Password"), redisSection.getString("Server-ID")))
                    .thenAccept((redisManager) -> {
                        if (redisManager == null) {
                            getLogger().severe("Unable to load redis connection, disabling.");
                            getServer().getPluginManager().disablePlugin(this);
                            return;
                        }

                        this.redisManager = redisManager;
                    });
        }

        if (!getConfig().getString("Hikari-Details.Address", "").isEmpty()) {
            ConfigurationSection hikariSection = getConfig().getConfigurationSection("Hikari-Details");

            CompletableFuture.supplyAsync(() ->
                            new HikariAPI(hikariSection.getString("Address"), hikariSection.getString("Database"),
                                    hikariSection.getString("Port"), hikariSection.getString("Username"), hikariSection.getString("Password")))
                    .thenAccept((api) -> {
                        if (api == null) {
                            getLogger().severe("Unable to load hikari connection, disabling.");
                            getServer().getPluginManager().disablePlugin(this);
                            return;
                        }

                        hikariAPI = api;
                    }).exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        }

        actionManager = new ActionManager(this);
        menuManager = new MenuManager(this);

        //UUIDUtil.initialize(getDataFolder());

        getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("Ignored-Materials")), this);
        getServer().getPluginManager().registerEvents(new DispenserArmorListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerListener(), this);
    }

    @Override
    public void onDisable() {
        if (redisManager != null && redisManager.getRedissonClient() != null) {
            redisManager.getRedissonClient().shutdown();
        }
    }

    public void toggleDebug() {
        debug = !debug;
        redisManager.setDebug(debug);
    }

}
