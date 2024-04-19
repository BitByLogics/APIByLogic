package net.bitbylogic.apibylogic.database.redis.listener;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.database.redis.timed.RedisTimedResponse;
import net.bitbylogic.apibylogic.database.redis.client.RedisClient;
import net.bitbylogic.apibylogic.database.redis.timed.RedisTimedRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
public class ListenerComponent {

    @Setter
    private RedisClient source;

    private final String target;
    private final String channel;

    private final HashMap<String, String> data;
    private final HashMap<RedisTimedRequest, Long> timedRequests = new HashMap<>();
    private final List<RedisTimedResponse> timedResponses = new ArrayList<>();
    private boolean allowRequestSelfActivation;

    public ListenerComponent() {
        this(null, null);
    }

    public ListenerComponent(String target, String channel) {
        this(target, channel, new HashMap<>());
    }

    public ListenerComponent(String target, String channel, HashMap<String, String> data) {
        this.target = target;
        this.channel = channel;
        this.data = data;
    }

    public ListenerComponent addData(String key, Object object) {
        data.put(key, new Gson().toJson(object));
        return this;
    }

    public ListenerComponent addDataRaw(String key, String value) {
        data.put(key, value);
        return this;
    }

    public ListenerComponent addTimedRequest(TimeUnit unit, int time, RedisTimedRequest request) {
        timedRequests.put(request, unit.toMillis(time));
        return this;
    }

    public ListenerComponent addTimedResponse(RedisTimedResponse response) {
        timedResponses.add(response);
        return this;
    }

    public ListenerComponent selfActivation(boolean selfActivation) {
        this.allowRequestSelfActivation = selfActivation;
        return this;
    }

    public <T> T getData(String key, Class<T> type) {
        if (!data.containsKey(key)) {
            return null;
        }

        return new Gson().fromJson(data.get(key), type);
    }

    public RedisTimedRequest getRequestByID(String id) {
        return timedRequests.keySet().stream().filter(request -> request.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

}
