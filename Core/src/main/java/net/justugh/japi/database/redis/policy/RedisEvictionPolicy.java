package net.justugh.japi.database.redis.policy;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.EvictionPolicy;

public class RedisEvictionPolicy<T> implements EvictionPolicy<T> {

    @Override
    public boolean evict(EvictionConfig config, PooledObject<T> underTest, int idleCount) {
        return (config.getIdleSoftEvictTime() < underTest.getIdleTimeMillis() &&
                config.getMinIdle() < idleCount) ||
                config.getIdleEvictTime() < underTest.getIdleTimeMillis();
    }
}
