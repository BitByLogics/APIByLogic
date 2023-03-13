package net.justugh.japi;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.database.redis.RedisManager;
import net.justugh.japi.type.ProxyType;

@Getter
@Setter
public class JustAPIProxy {

    @Getter
    private static JustAPIProxy instance;

    private RedisManager redisManager;
    private ProxyType proxyType;

    public JustAPIProxy(RedisManager redisManager, ProxyType proxyType) {
        instance = this;

        this.redisManager = redisManager;
        this.proxyType = proxyType;
    }

    public static void initialize(RedisManager redisManager, ProxyType proxyType) {
        new JustAPIProxy(redisManager, proxyType);
    }

}
