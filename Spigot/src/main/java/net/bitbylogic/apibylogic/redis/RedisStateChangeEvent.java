package net.bitbylogic.apibylogic.redis;

import lombok.Getter;
import net.bitbylogic.apibylogic.database.redis.RedisManager;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class RedisStateChangeEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final RedisState state;
    private final RedisManager redisManager;

    public RedisStateChangeEvent(RedisState state, RedisManager redisManager) {
        this.state = state;
        this.redisManager = redisManager;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public enum RedisState {
        CONNECTED,
        DISCONNECTED;
    }

}
