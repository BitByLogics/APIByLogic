package net.justugh.japi.database.redis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.database.redis.client.RedisClient;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Getter
public class RedisManager {

    private RedissonClient redissonClient;
    private final List<RedisClient> clients;

    @Getter(AccessLevel.NONE)
    private final String SOURCE_ID;

    private final Gson gson;
    @Setter
    private boolean debug;

    public RedisManager(String host, int port, String password, String sourceId) {
        this.SOURCE_ID = sourceId;
        this.gson = new GsonBuilder().create();

        this.clients = new ArrayList<>();

        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format("redis://%s:%s", host, port))
                .setPassword(password.isEmpty() ? null : password)
                .setPingConnectionInterval(50)
                .setConnectTimeout(20_000)
                .setTimeout(25_000_000)
                .setRetryInterval(750)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16);

        try {
            redissonClient = Redisson.create(config);
        } catch (Exception exception) {
            Logger.getGlobal().severe("[REDIS]: Unable to connect to redis, contact developer with error below.");
            exception.printStackTrace();
        }
    }

    /**
     * Registers a new RedisClient. Used to send and receive
     * messages.
     *
     * @param id The id for the RedisClient.
     * @return The new RedisClient instance.
     */
    public RedisClient registerClient(String id) {
        if (clients.stream().anyMatch(client -> client.getID().equalsIgnoreCase(id))) {
            Logger.getGlobal().warning(String.format("[REDIS]: Attempted to register RedisClient with duplicate ID '%s', contact developer.", id));
            return clients.stream().filter(client -> client.getID().equalsIgnoreCase(id)).findFirst().orElse(null);
        }

        RedisClient client = new RedisClient(this, id);
        clients.add(client);

        return client;
    }

    /**
     * Get the source id. Used to identify where a message
     * is sent and received from.
     *
     * @return The source id.
     */
    public String getSourceID() {
        return SOURCE_ID;
    }

}
