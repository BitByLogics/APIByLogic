package net.justugh.japi.database.redis.listener;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.database.redis.client.RedisClient;

import java.util.HashMap;

@Getter
public class ListenerComponent {

    @Setter
    private RedisClient source;

    private final String target;
    private final String channel;

    private final HashMap<String, String> data;

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

    public <T> T getData(String key, Class<T> type) {
        if (!data.containsKey(key)) {
            return null;
        }

        return new Gson().fromJson(data.get(key), type);
    }

}
