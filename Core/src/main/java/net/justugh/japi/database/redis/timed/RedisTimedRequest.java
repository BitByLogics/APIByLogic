package net.justugh.japi.database.redis.timed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.util.Callback;

import java.util.UUID;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class RedisTimedRequest {

    private final UUID uniqueId;

    private final String id;
    private final transient Callback<ListenerComponent> successCallback;
    private final transient Callback<Void> timeoutCallback;

    @Setter
    private String channel;

    public RedisTimedRequest(String id, Callback<ListenerComponent> successCallback, Callback<Void> timeoutCallback) {
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
