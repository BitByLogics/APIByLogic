package net.bitbylogic.apibylogic;

import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.type.ProxyType;
import net.bitbylogic.apibylogic.database.redis.RedisManager;

@Getter
@Setter
public class APIByLogicProxy {

    @Getter
    private static APIByLogicProxy instance;

    private RedisManager redisManager;
    private ProxyType proxyType;

    public APIByLogicProxy(RedisManager redisManager, ProxyType proxyType) {
        instance = this;

        this.redisManager = redisManager;
        this.proxyType = proxyType;
    }

    public static void initialize(RedisManager redisManager, ProxyType proxyType) {
        new APIByLogicProxy(redisManager, proxyType);
    }

}
