package net.bitbylogic.apibylogic;

import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.action.ActionManager;
import net.bitbylogic.apibylogic.command.APIByLogicCommand;
import net.bitbylogic.apibylogic.database.hikari.HikariAPI;
import net.bitbylogic.apibylogic.database.redis.RedisManager;
import net.bitbylogic.apibylogic.database.redis.client.RedisClient;
import net.bitbylogic.apibylogic.listener.SpawnerListener;
import net.bitbylogic.apibylogic.menu.listener.MenuListener;
import net.bitbylogic.apibylogic.redis.PlayerMessageListener;
import net.bitbylogic.apibylogic.redis.RedisStateChangeEvent;
import net.bitbylogic.apibylogic.scoreboard.LogicScoreboard;
import net.bitbylogic.apibylogic.util.Callback;
import net.bitbylogic.apibylogic.util.ItemStackUtil;
import net.bitbylogic.apibylogic.util.event.armor.listener.ArmorListener;
import net.bitbylogic.apibylogic.util.event.armor.listener.DispenserArmorListener;
import net.bitbylogic.apibylogic.util.message.LogicColor;
import net.bitbylogic.apibylogic.util.request.LogicRequest;
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
public class APIByLogic extends JavaPlugin {

    @Getter
    private static APIByLogic instance;

    @Setter
    private boolean debug = false;

    private RedisManager redisManager;
    private RedisClient redisClient;
    private HikariAPI hikariAPI;

    private ActionManager actionManager;

    private final List<LogicScoreboard> activeBoards = new ArrayList<>();
    private final ConcurrentLinkedQueue<LogicRequest> pendingTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        LogicColor.loadColors(getConfig());
        ItemStackUtil.initialize(this);
        getCommand("apibylogic").setExecutor(new APIByLogicCommand());

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
                        this.redisClient = redisManager.registerClient("APIByLogic");

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

            Iterator<LogicRequest> taskIterator = pendingTasks.iterator();

            while (taskIterator.hasNext()) {
                LogicRequest request = taskIterator.next();

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
