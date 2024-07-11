package net.bitbylogic.apibylogic.database.redis.timed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;

import java.util.UUID;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class RedisTimedRequest {

    private final UUID uniqueId;

    private final String id;
    private final transient Consumer<ListenerComponent> successCallback;
    private final transient Consumer<Void> timeoutCallback;

    @Setter
    private String channel;

    public RedisTimedRequest(String id, Consumer<ListenerComponent> successCallback, Consumer<Void> timeoutCallback) {
        this.uniqueId = UUID.randomUUID();
        this.id = id;
        this.successCallback = successCallback;
        this.timeoutCallback = timeoutCallback;
    }

    public RedisTimedRequest(UUID uniqueId, String id, String channel) {
        this.uniqueId = uniqueId;
        this.id = id;
        this.successCallback = null;
        this.timeoutCallback = null;
        this.channel = channel;
    }

}
