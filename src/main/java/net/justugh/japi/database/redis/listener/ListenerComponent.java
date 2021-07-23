package net.justugh.japi.database.redis.listener;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
public class ListenerComponent {

    @Setter
    private String source;

    private final String target;
    private final String channel;
    private final String message;

    private final HashMap<String, Object> data;

    public ListenerComponent(String target, String channel, String message) {
        this(target, channel, message, Maps.newHashMap());
    }

    public ListenerComponent(String target, String channel, String message, HashMap<String, Object> data) {
        this.target = target;
        this.channel = channel;
        this.message = message;
        this.data = data;
    }

}
