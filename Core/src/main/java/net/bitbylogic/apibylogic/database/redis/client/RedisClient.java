package net.bitbylogic.apibylogic.database.redis.client;

import lombok.Getter;
import net.bitbylogic.apibylogic.database.redis.RedisManager;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.database.redis.listener.RedisMessageListener;
import net.bitbylogic.apibylogic.database.redis.timed.RedisTimedRequest;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

        getTopic(REQUEST_TOPIC_ID).thenAcceptAsync(topic -> {
            topic.addListener(String.class, (channel, msg) -> {
                ListenerComponent component = redisManager.getGson().fromJson(msg, ListenerComponent.class);

                if (redisManager.isDebug()) {
                    Logger.getGlobal().info(String.format("[INCOMING]: %s -> %s: %s", component.getSource().getID(), component.getTarget(), msg));
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
            });
        });
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
        Consumer<ListenerComponent> componentConsumer = listener::onReceive;

        getTopic(listener.getChannelName()).thenAcceptAsync(rTopic -> rTopic.addListener(String.class, (channel, msg) -> {
            try {
                ListenerComponent component = redisManager.getGson().fromJson(msg, ListenerComponent.class);

                if (redisManager.isDebug()) {
                    Logger.getGlobal().info(String.format("[INCOMING]: %s -> %s: %s", component.getSource().getID(), component.getTarget(), msg));
                }

                if (component.getTarget() == null || component.getTarget().isEmpty()) {
                    listeners.stream().filter(l -> l.getChannelName().equalsIgnoreCase(component.getChannel()))
                            .forEach(l -> {
                                if (component.getSource().getServerId().equalsIgnoreCase(l.getClient().getServerId()) && !l.isSelfActivation()) {
                                    return;
                                }

                                if (redisManager.isDebug()) {
                                    Logger.getGlobal().info(String.format("[INCOMING]: %s -> %s: %s", component.getSource().getID(), component.getTarget(), msg));
                                }

                                componentConsumer.accept(component);
                            });
                    return;
                }

                if (!redisManager.getSourceID().equalsIgnoreCase(component.getTarget())) {
                    return;
                }

                listeners.stream().filter(l -> l.getChannelName().equalsIgnoreCase(component.getChannel())).forEach(l -> {
                    if (redisManager.isDebug()) {
                        Logger.getGlobal().info(String.format("[INCOMING]: %s -> %s: %s", component.getSource().getID(), component.getTarget(), msg));
                    }

                    componentConsumer.accept(component);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
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
            Logger.getGlobal().info(String.format("[OUTGOING]: %s -> %s: %s", component.getSource().getID(), component.getTarget(), redisManager.getGson().toJson(component)));
        }

        getTopic(component.getChannel()).thenAcceptAsync(topic -> topic.publish(redisManager.getGson().toJson(component)));
    }

    public void sendTimedResponse(ListenerComponent component) {
        component.setSource(this);

        if (component.getTimedResponses().isEmpty()) {
            Logger.getGlobal().warning("Attempted to send response with no responses, skipped.");
            return;
        }

        if (redisManager.isDebug()) {
            Logger.getGlobal().info(String.format("[TIMED RESPONSE]: %s -> %s: %s", component.getSource().getID(), component.getTarget(), redisManager.getGson().toJson(component)));
        }

        getTopic(REQUEST_TOPIC_ID).thenAcceptAsync(topic -> topic.publish(redisManager.getGson().toJson(component)));
    }

    private CompletableFuture<RTopic> getTopic(String topicName) {
        return CompletableFuture.supplyAsync(() -> redisManager.getRedissonClient().getTopic(topicName));
    }

    public RedissonClient getRedisClient() {
        return redisManager.getRedissonClient();
    }

}
