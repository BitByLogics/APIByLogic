package net.justugh.japi;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.action.ActionManager;
import net.justugh.japi.command.JustAPICommand;
import net.justugh.japi.database.hikari.HikariAPI;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.listener.SpawnerListener;
import net.justugh.japi.menu.listener.MenuListener;
import net.justugh.japi.redis.PlayerMessageListener;
import net.justugh.japi.redis.RedisStateChangeEvent;
import net.justugh.japi.scoreboard.JustBoard;
import net.justugh.japi.util.Callback;
import net.justugh.japi.util.ItemStackUtil;
import net.justugh.japi.util.event.armor.listener.ArmorListener;
import net.justugh.japi.util.event.armor.listener.DispenserArmorListener;
import net.justugh.japi.util.request.JustAPIRequest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class JustAPIPlugin extends JavaPlugin {

    @Getter
    private static JustAPIPlugin instance;

    @Setter
    private boolean debug = false;

    private RedisManager redisManager;
    private RedisClient redisClient;
    private HikariAPI hikariAPI;

    private ActionManager actionManager;

    private final List<JustBoard> activeBoards = new ArrayList<>();
    private final ConcurrentLinkedQueue<JustAPIRequest> pendingTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        ItemStackUtil.initialize(this);
        getCommand("justapi").setExecutor(new JustAPICommand());

        if (!getConfig().getString("Redis-Credentials.Host", "").isEmpty()) {
            ConfigurationSection redisSection = getConfig().getConfigurationSection("Redis-Credentials");

            CompletableFuture.supplyAsync(() -> new RedisManager(
                            redisSection.getString("Host"), redisSection.getInt("Port"),
                            redisSection.getString("Password"), getConfig().getString("Server-ID")))
                    .thenAccept((redisManager) -> {
                        if (redisManager == null) {
                            getLogger().severe("Unable to load redis connection, disabling.");
                            getServer().getPluginManager().disablePlugin(this);
                            return;
                        }

                        this.redisManager = redisManager;
                        this.redisClient = redisManager.registerClient("JustAPI");

                        Bukkit.getPluginManager().callEvent(new RedisStateChangeEvent(RedisStateChangeEvent.RedisState.CONNECTED, redisManager));

                        redisClient.registerListener(new PlayerMessageListener());
                    });
        }

        if (!getConfig().getString("Hikari-Details.Address", "").isEmpty() && !getConfig().getString("Hikari-Details.Database", "").isEmpty()) {
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

        //UUIDUtil.initialize(getDataFolder());

        getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("Ignored-Materials")), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new DispenserArmorListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnerListener(), this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            if (pendingTasks.isEmpty()) {
                return;
            }

            Iterator<JustAPIRequest> taskIterator = pendingTasks.iterator();

            while (taskIterator.hasNext()) {
                JustAPIRequest request = taskIterator.next();

                switch (request.getType()) {
                    case REDIS:
                        if (redisManager == null) {
                            continue;
                        }

                        request.getCallback().call(redisManager);
                        taskIterator.remove();
                        break;
                    case HIKARI:
                        if (hikariAPI == null) {
                            continue;
                        }

                        request.getCallback().call(hikariAPI);
                        taskIterator.remove();
                        break;
                    default:
                        taskIterator.remove();
                        break;
                }
            }
        }, 0, 5);
    }

    @Override
    public void onDisable() {
        if (redisManager != null && redisManager.getRedissonClient() != null) {
            redisManager.getRedissonClient().shutdown();
        }
    }

    /**
     * Request an instance of a HikariAPI object, with a database.
     *
     * @param database The database to use
     * @param callback The callback to call on object creation
     */
    public void requestHikariAPI(String database, Callback<Optional<HikariAPI>> callback) {
        if (getConfig().getString("Hikari-Details.Address", "").isEmpty()) {
            callback.call(Optional.empty());
            return;
        }

        ConfigurationSection hikariSection = getConfig().getConfigurationSection("Hikari-Details");

        CompletableFuture.supplyAsync(() ->
                        new HikariAPI(hikariSection.getString("Address"), database,
                                hikariSection.getString("Port"), hikariSection.getString("Username"), hikariSection.getString("Password")))
                .thenAccept((api) -> {
                    if (api == null) {
                        getLogger().severe("Unable to supply HikariAPI object, ensure configuration is correct.");
                        return;
                    }

                    callback.call(Optional.of(api));
                }).exceptionally(e -> {
                    e.printStackTrace();
                    callback.call(Optional.empty());
                    return null;
                });
    }

    public void toggleDebug() {
        debug = !debug;
        redisManager.setDebug(debug);
    }

}
