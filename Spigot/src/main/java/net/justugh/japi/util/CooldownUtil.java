package net.justugh.japi.util;

import com.google.common.collect.Lists;
import net.justugh.japi.JustAPIPlugin;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CooldownUtil {

    private static final List<String> cooldowns = Lists.newArrayList();

    public static void startCooldown(String key, UUID player) {
        cooldowns.add(key + "-" + player);
    }

    public static void startCooldown(String key, UUID player, long expireTime, TimeUnit unit) {
        cooldowns.add(key + "-" + player);

        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> cooldowns.remove(key + "-" + player), unit.toSeconds(expireTime) * 20);
    }

    public static void endCooldown(String key, UUID player) {
        cooldowns.remove(key + "-" + player);
    }

    public static boolean hasCooldown(String key, UUID player) {
        return cooldowns.contains(key + "-" + player);
    }

}
