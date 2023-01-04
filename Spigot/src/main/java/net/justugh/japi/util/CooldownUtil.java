package net.justugh.japi.util;

import com.google.common.collect.Lists;
import net.justugh.japi.JustAPIPlugin;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CooldownUtil {

    private static final List<String> cooldowns = Lists.newArrayList();

    public static void startCooldown(String key, UUID identifier) {
        cooldowns.add(key + "-" + identifier);
    }

    public static void startCooldown(String key, UUID identifier, long expireTime, TimeUnit unit) {
        cooldowns.add(key + "-" + identifier);

        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> {
            cooldowns.remove(key + "-" + identifier);
        }, unit.toSeconds(expireTime) * 20);
    }

    public static void startCooldown(String key, UUID identifier, long expireTime, TimeUnit unit, Callback<Void> completeCallback) {
        cooldowns.add(key + "-" + identifier);

        Bukkit.getScheduler().runTaskLater(JustAPIPlugin.getInstance(), () -> {
            cooldowns.remove(key + "-" + identifier);
            completeCallback.call(null);
        }, unit.toSeconds(expireTime) * 20);
    }

    public static void endCooldown(String key, UUID identifier) {
        cooldowns.remove(key + "-" + identifier);
    }

    public static boolean hasCooldown(String key, UUID identifier) {
        return cooldowns.contains(key + "-" + identifier);
    }

}
