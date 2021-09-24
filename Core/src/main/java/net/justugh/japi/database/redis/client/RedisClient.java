package net.justugh.japi.database.redis.client;

import lombok.Getter;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RedisClient {

    private final transient RedisManager redisManager;
    private final String ID;
    private final transient List<RedisMessageListener> listeners;

    public RedisClient(RedisManager redisManager, String ID) {
        this.redisManager = redisManager;
        this.ID = ID;
        listeners = new ArrayList<>();
    }

    /**
     * Registers a new RedisMessageListener as a valid listener.
     *
     * @param listener The listener to register.
     */
    public void registerListener(RedisMessageListener listener) {
        listeners.add(listener);
    }

    /**
     * Publishes a new message to be sent out to all listeners.
     *
     * @param component The information to send.
     */
    public void sendListenerMessage(ListenerComponent component) {
        component.setSource(this);
        String json = redisManager.getGson().toJson(component);

        try (Jedis jedis = redisManager.getJedisPool().getResource()) {
            jedis.publish(redisManager.getListenerChannel(), json);
        }
    }

}
