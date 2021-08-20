package net.justugh.japi.util;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;

public class CooldownUtil {

    private static final List<String> cooldowns = Lists.newArrayList();

    public static void startCooldown(String key, UUID player) {
        cooldowns.add(key + "-" + player);
    }

    public static void endCooldown(String key, UUID player) {
        cooldowns.remove(key + "-" + player);
    }

    public static boolean hasCooldown(String key, UUID player) {
        return cooldowns.contains(key + "-" + player);
    }

}
