package net.bitbylogic.apibylogic.database.redis.gson;

import com.google.gson.*;
import net.bitbylogic.apibylogic.database.redis.timed.RedisTimedRequest;

import java.lang.reflect.Type;
import java.util.UUID;

public class TimedRequestSerializer implements JsonSerializer<RedisTimedRequest>, JsonDeserializer<RedisTimedRequest> {

    @Override
    public JsonElement serialize(RedisTimedRequest src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("uniqueId", src.getUniqueId().toString());
        object.addProperty("id", src.getId());
        object.addProperty("channel", src.getChannel());
        return object;
    }

    @Override
    public RedisTimedRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();

        UUID uniqueId = UUID.fromString(object.get("uniqueId").getAsString());
        String id = object.get("id").getAsString();
        String channel = object.get("channel") == null ? null : object.get("channel").getAsString();

        return new RedisTimedRequest(uniqueId, id, channel);
    }

}
