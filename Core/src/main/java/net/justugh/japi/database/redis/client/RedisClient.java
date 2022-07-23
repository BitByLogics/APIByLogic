package net.justugh.japi.database.redis.client;

import lombok.Getter;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Getter
public class RedisClient {

    private final transient RedisManager redisManager;
    private final String serverId;
    private final String ID;
    private final transient List<RedisMessageListener> listeners;

    public RedisClient(RedisManager redisManager, String ID) {
        this.redisManager = redisManager;
        this.serverId = redisManager.getSourceID();
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

        if (redisManager.getRedissonClient().getTopic(listener.getChannelName()).countListeners() >= 1) {
            return;
        }

        redisManager.getRedissonClient().getTopic(listener.getChannelName()).addListener(String.class, (channel, msg) -> {
            try {
                ListenerComponent component = redisManager.getGson().fromJson(msg, ListenerComponent.class);

                if (component.getTarget() == null) {
                    listeners.stream().filter(l -> l.getChannelName().equalsIgnoreCase(component.getChannel()))
                            .forEach(l -> l.onReceive(component));
                    return;
                }

                if (!redisManager.getSourceID().equalsIgnoreCase(component.getTarget())) {
                    return;
                }

                if (redisManager.isDebug()) {
                    Logger.getGlobal().info(String.format("%s -> %s (%s): %s", component.getSource().getID(), component.getTarget(), component.getChannel(), component.getData().toString()));
                }

                listeners.stream().filter(l -> l.getChannelName().equalsIgnoreCase(component.getChannel())).forEach(l -> l.onReceive(component));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Publishes a new message to be sent out to all listeners.
     *
     * @param component The information to send.
     */
    public void sendListenerMessage(ListenerComponent component) {
        component.setSource(this);

        if (redisManager.isDebug()) {
            Logger.getGlobal().info(String.format("%s -> %s (%s): %s", component.getSource().getID(), component.getTarget(), component.getChannel(), component.getData().toString()));
        }

        redisManager.getRedissonClient().getTopic(component.getChannel()).publish(redisManager.getGson().toJson(component));
    }

    public RedissonClient getRedisClient() {
        return redisManager.getRedissonClient();
    }

}
