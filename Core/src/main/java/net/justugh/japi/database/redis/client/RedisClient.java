package net.justugh.japi.database.redis.client;

import lombok.Getter;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import net.justugh.japi.database.redis.timed.RedisTimedRequest;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.logging.Logger;

@Getter
public class RedisClient {

    private final String REQUEST_TOPIC_ID = "INTERNAL_REDIS_REQUESTS";

    private final transient RedisManager redisManager;
    private final String serverId;
    private final String ID;
    private final transient List<RedisMessageListener> listeners;
    private final transient HashMap<RedisTimedRequest, Timer> timedRequests;

    public RedisClient(RedisManager redisManager, String ID) {
        this.redisManager = redisManager;
        this.serverId = redisManager.getSourceID();
        this.ID = ID;
        this.listeners = new ArrayList<>();
        this.timedRequests = new HashMap<>();

        redisManager.getRedissonClient().getTopic(REQUEST_TOPIC_ID).addListener(String.class, ((channel, msg) -> {
            ListenerComponent component = redisManager.getGson().fromJson(msg, ListenerComponent.class);

            if (redisManager.isDebug()) {
                Logger.getGlobal().info(String.format("%s -> %s: %s", component.getSource().getID(), component.getTarget(), msg));
            }

            component.getTimedResponses().forEach(response -> {
                if (component.getSource().getID().equalsIgnoreCase(redisManager.getSourceID()) && !component.isAllowRequestSelfActivation()) {
                    return;
                }

                RedisTimedRequest timedRequest = timedRequests.keySet().stream().filter(request -> request.getUniqueId().equals(response.getUniqueId()))
                        .findFirst().orElse(null);

                if (timedRequest == null) {
                    return;
                }

                timedRequests.get(timedRequest).cancel();
                timedRequests.remove(timedRequest);
                timedRequest.getSuccessCallback().call(component);
            });
        }));
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

        listener.setClient(this);
        redisManager.getRedissonClient().getTopic(listener.getChannelName()).addListener(String.class, (channel, msg) -> {
            try {
                ListenerComponent component = redisManager.getGson().fromJson(msg, ListenerComponent.class);

                if (redisManager.isDebug()) {
                    Logger.getGlobal().info(String.format("%s -> %s: %s", component.getSource().getID(), component.getTarget(), msg));
                }

                if (component.getTarget() == null || component.getTarget().isEmpty()) {
                    listeners.stream().filter(l -> l.getChannelName().equalsIgnoreCase(component.getChannel()))
                            .forEach(l -> {
                                if (component.getSource().getID().equalsIgnoreCase(redisManager.getSourceID()) && !l.isSelfActivation()) {
                                    return;
                                }

                                l.onReceive(component);
                            });
                    return;
                }

                if (!redisManager.getSourceID().equalsIgnoreCase(component.getTarget())) {
                    return;
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
        component.getTimedRequests().forEach((request, expireTime) -> {
            if (expireTime == -1) {
                timedRequests.put(request, null);
                return;
            }

            final Timer requestTimer = new Timer();
            requestTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timedRequests.remove(request);
                    request.getTimeoutCallback().call(null);
                    requestTimer.cancel();
                }
            }, expireTime);

            timedRequests.put(request, requestTimer);
        });

        if (redisManager.isDebug()) {
            Logger.getGlobal().info(String.format("%s -> %s: %s", component.getSource().getID(), component.getTarget(), redisManager.getGson().toJson(component)));
        }

        redisManager.getRedissonClient().getTopic(component.getChannel()).publish(redisManager.getGson().toJson(component));
    }

    public void sendTimedResponse(ListenerComponent component) {
        if (component.getTimedResponses().isEmpty()) {
            Logger.getGlobal().warning("Attempted to send response with no responses, skipped.");
            return;
        }

        if (redisManager.isDebug()) {
            Logger.getGlobal().info(String.format("%s -> %s: %s", component.getSource().getID(), component.getTarget(), redisManager.getGson().toJson(component)));
        }

        component.setSource(this);
        redisManager.getRedissonClient().getTopic(REQUEST_TOPIC_ID).publish(redisManager.getGson().toJson(component));
    }

    public RedissonClient getRedisClient() {
        return redisManager.getRedissonClient();
    }

}
