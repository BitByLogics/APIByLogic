package net.justugh.japi.database.redis;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

@Setter
@Getter
public class RedisManager {

    private final JedisPool jedisPool;
    private final List<RedisClient> clients;

    private final String LISTENER_CHANNEL = "japi_channel";

    private final Gson gson;
    private boolean debugMode;

    public RedisManager(String host, int port, String password) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setEvictionPolicyClassName("net.justugh.japi.database.redis.policy.RedisEvictionPolicy");
        config.setMaxTotal(25);
        config.setMaxIdle(10);
        config.setMinIdle(5);

        this.jedisPool = new JedisPool(config, host, port, 10_000, password);
        this.gson = new Gson();

        this.clients = Lists.newArrayList();

        Thread.UncaughtExceptionHandler errHandler = (thread, error) -> {
            Bukkit.getLogger().severe("(Redis): Error: " + error + ", Line Number: " + error.getStackTrace()[0].getLineNumber());
            error.printStackTrace(System.out);
        };

        Thread subThread = new Thread(() -> jedisPool.getResource().subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleMessage(message);
            }
        }, LISTENER_CHANNEL));

        subThread.setUncaughtExceptionHandler(errHandler);
        subThread.start();
    }

    /**
     * Registers a new RedisClient. Used to send and receive
     * messages.
     *
     * @param plugin The plugin whose name should be used.
     * @return The new RedisClient instance.
     */
    public RedisClient registerClient(JavaPlugin plugin) {
        if (clients.stream().anyMatch(client -> client.getID().equalsIgnoreCase(plugin.getName()))) {
            Bukkit.getLogger().warning("[REDIS]: Plugin '" + plugin.getName() + "' attempted to recreate RedisClient, contact developer.");
            return clients.stream().filter(client -> client.getID().equalsIgnoreCase(plugin.getName())).findFirst().orElse(null);
        }

        RedisClient client = new RedisClient(plugin.getName());
        clients.add(client);

        return client;
    }

    /**
     * Handles all incoming Redis messages to be rerouted
     * to listeners.
     *
     * @param message The message to reroute.
     */
    private void handleMessage(String message) {
        ListenerComponent component = gson.fromJson(message, ListenerComponent.class);

        if (component.getTarget() == null) {
            clients.stream().filter(client ->
                    !client.getID().equalsIgnoreCase(component.getSource())).forEach(client ->
                    client.getListeners().stream().filter(listener ->
                            listener.getChannelName().equalsIgnoreCase(component.getChannel())).forEach(listener -> listener.onReceive(component)));
        }

        clients.stream().filter(client -> !client.getID().equalsIgnoreCase(component.getSource())).forEach(client -> {
            if (debugMode) {
                Bukkit.getLogger().info(Format.format("&a" + component.getSource() + " &8-> &a" +
                        component.getTarget() + " &8(&e" + component.getChannel() + "&8)&8: &f" + component.getMessage()));
            }

            client.getListeners().stream().filter(listener ->
                    listener.getChannelName().equalsIgnoreCase(component.getChannel())).forEach(listener -> listener.onReceive(component));
        });
    }

}
