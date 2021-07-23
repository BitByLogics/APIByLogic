package net.justugh.japi.database.redis.client;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.justugh.japi.JustAPI;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;

import java.util.List;

@Getter
public class RedisClient {

    private final String ID;
    private final List<RedisMessageListener> listeners;

    public RedisClient(String ID) {
        this.ID = ID;
        listeners = Lists.newArrayList();
    }

    /**
     * Registers a new RedisMessageListener as a valid listener.
     *
     * @param listener The listener to register.
     */
    public void registerListener(RedisMessageListener listener) {
        Bukkit.getLogger().info("(" + ID + ") [REDIS] Registered listener " + listener.getClass().getSimpleName());
        listeners.add(listener);
    }

    /**
     * Publishes a new message to be sent out to all listeners.
     *
     * @param component The information to send.
     */
    public void sendListenerMessage(ListenerComponent component) {
        component.setSource(ID);
        String json = JustAPI.getInstance().getRedisManager().getGson().toJson(component);

        try (Jedis jedis = JustAPI.getInstance().getRedisManager().getJedisPool().getResource()) {
            jedis.publish(JustAPI.getInstance().getRedisManager().getLISTENER_CHANNEL(), json);
        }
    }

}
