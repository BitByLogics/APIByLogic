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
import net.bitbylogic.apibylogic.metrics.MetricsWrapper;
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
import org.bukkit.plugin.PluginManager;
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
    private boolean debugMode = false;

    private RedisManager redisManager;
    private RedisClient redisClient;
    private HikariAPI hikariAPI;

    private ActionManager actionManager;
    private MetricsWrapper metricsWrapper;

    private final List<LogicScoreboard> activeBoards = new ArrayList<>();
    private final ConcurrentLinkedQueue<LogicRequest> pendingTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        LogicColor.loadColors(getConfig());
        ItemStackUtil.initialize(this);

        getCommand("apibylogic").setExecutor(new APIByLogicCommand());

        initializeRedis();
        initializeHikari();

        actionManager = new ActionManager(this);

        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new ArmorListener(getConfig().getStringList("Ignored-Materials")), this);
        pluginManager.registerEvents(new MenuListener(), this);
        pluginManager.registerEvents(new DispenserArmorListener(), this);
        pluginManager.registerEvents(new SpawnerListener(), this);

        // Possibly remove this later? I forget why I added it
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

        if (getConfig().getBoolean("Track-Metrics", true)) {
            metricsWrapper = new MetricsWrapper(this);
            getLogger().info("Thank you for allowing metric tracking!");
        }
    }

    @Override
    public void onDisable() {
        if (redisManager != null && redisManager.getRedissonClient() != null) {
            redisManager.getRedissonClient().shutdown();
        }

        if (hikariAPI != null && !hikariAPI.getHikari().isClosed()) {
            hikariAPI.getHikari().close();
        }
    }

    private void initializeRedis() {
        if (!getConfig().isSet("Redis-Credentials.Host")) {
            return;
        }

        ConfigurationSection redisSection = getConfig().getConfigurationSection("Redis-Credentials");

        if (redisSection == null) {
            getLogger().severe("Redis credentials section missing, redis will not function.");
            return;
        }

        CompletableFuture.supplyAsync(() -> new RedisManager(
                        redisSection.getString("Host"), redisSection.getInt("Port"),
                        redisSection.getString("Password"), getConfig().getString("Server-ID")))
                .thenAccept((redisManager) -> {
                    if (redisManager == null) {
                        getLogger().severe("Unable to connect to redis, disabling.");
                        getServer().getPluginManager().disablePlugin(this);
                        return;
                    }

                    this.redisManager = redisManager;
                    this.redisClient = redisManager.registerClient("APIByLogic");

                    Bukkit.getPluginManager().callEvent(new RedisStateChangeEvent(RedisStateChangeEvent.RedisState.CONNECTED, redisManager));

                    redisClient.registerListener(new PlayerMessageListener());
                });
    }

    private void initializeHikari() {
        if (!getConfig().isSet("Hikari-Details.Address") || !getConfig().isSet("Hikari-Details.Database")) {
            return;
        }

        ConfigurationSection hikariSection = getConfig().getConfigurationSection("Hikari-Details");

        if (hikariSection == null) {
            return;
        }

        CompletableFuture.supplyAsync(() -> new HikariAPI(
                        hikariSection.getString("Address"), hikariSection.getString("Database"),
                        hikariSection.getString("Port"), hikariSection.getString("Username"),
                        hikariSection.getString("Password")))
                .thenAccept((hikariAPI) -> {
                    if (hikariAPI == null) {
                        getLogger().severe("Unable to connect to hikari, disabling.");
                        getServer().getPluginManager().disablePlugin(this);
                        return;
                    }

                    this.hikariAPI = hikariAPI;
                }).exceptionally(e -> {
                    getLogger().severe("Error connecting to hikari: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                });
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

        CompletableFuture.supplyAsync(() -> new HikariAPI(
                        hikariSection.getString("Address"), database,
                        hikariSection.getString("Port"), hikariSection.getString("Username"),
                        hikariSection.getString("Password")))
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

    public void toggleDebugMode() {
        debugMode = !debugMode;
        redisManager.setDebug(debugMode);
    }

}
