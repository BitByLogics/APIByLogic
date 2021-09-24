package net.justugh.japi.database.redis;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.logging.Logger;

@Getter
public class RedisManager {

    private final JedisPool jedisPool;
    private final List<RedisClient> clients;

    @Getter(AccessLevel.NONE)
    private final String LISTENER_CHANNEL = "japi_channel";
    @Getter(AccessLevel.NONE)
    private final String SOURCE_ID;

    private final Gson gson;
    @Setter
    private boolean debug;

    public RedisManager(String host, int port, String password, String sourceId) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setEvictionPolicyClassName("net.justugh.japi.database.redis.policy.RedisEvictionPolicy");
        config.setMaxTotal(25);
        config.setMaxIdle(10);
        config.setMinIdle(5);

        this.jedisPool = password.isEmpty() ? new JedisPool(config, host, port, 10_000) : new JedisPool(config, host, port, 10_000, password);
        this.SOURCE_ID = sourceId;
        this.gson = new GsonBuilder().create();

        this.clients = Lists.newArrayList();

        Thread.UncaughtExceptionHandler errHandler = (thread, error) -> {
            Logger.getGlobal().severe("(Redis): Error: " + error + ", Line Number: " + error.getStackTrace()[0].getLineNumber());
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
     * @param id The id for the RedisClient.
     * @return The new RedisClient instance.
     */
    public RedisClient registerClient(String id) {
        if (clients.stream().anyMatch(client -> client.getID().equalsIgnoreCase(id))) {
            Logger.getGlobal().warning(String.format("[REDIS]: Attempted to register RedisClient with duplicate ID '%s', contact developer.", id));
            return clients.stream().filter(client -> client.getID().equalsIgnoreCase(id)).findFirst().orElse(null);
        }

        RedisClient client = new RedisClient(this, id);
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
                    !SOURCE_ID.equalsIgnoreCase(component.getSource().getID())).forEach(client ->
                    client.getListeners().stream().filter(listener ->
                            listener.getChannelName().equalsIgnoreCase(component.getChannel())).forEach(listener -> listener.onReceive(component)));
        }

        clients.stream().filter(client -> !client.getID().equalsIgnoreCase(component.getSource().getID())).forEach(client -> {
            if (debug) {
                Logger.getGlobal().info(String.format("%s -> %s (%s): %s", component.getSource(), component.getTarget(), component.getChannel(), component.getData().toString()));
            }

            client.getListeners().stream().filter(listener ->
                    listener.getChannelName().equalsIgnoreCase(component.getChannel())).forEach(listener -> listener.onReceive(component));
        });
    }

    /**
     * Get the listener channel id.
     *
     * @return The listener channel id.
     */
    public String getListenerChannel() {
        return LISTENER_CHANNEL;
    }

    /**
     * Get the source id. Used to identify where a message
     * is sent and received from.
     *
     * @return The source id.
     */
    public String getSourceID() {
        return SOURCE_ID;
    }

}
