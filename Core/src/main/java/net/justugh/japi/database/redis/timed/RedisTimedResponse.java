package net.justugh.japi.database.redis.timed;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class RedisTimedResponse {

    private final UUID uniqueId;
    private final String id;

    public RedisTimedResponse(RedisTimedRequest request) {
        this.uniqueId = request.getUniqueId();
        this.id = request.getId();
    }

}
