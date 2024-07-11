package net.bitbylogic.apibylogic.util;

import net.bitbylogic.apibylogic.util.cooldown.Cooldown;
import net.bitbylogic.apibylogic.APIByLogic;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CooldownUtil {

    private static final HashMap<UUID, List<Cooldown>> cooldowns = new HashMap<>();

    public static void startCooldown(String key, UUID identifier) {
        List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
        currentCooldowns.add(new Cooldown(identifier, key, -1));
        cooldowns.put(identifier, currentCooldowns);
    }

    public static void startCooldown(String key, UUID identifier, long expireTime, TimeUnit unit) {
        List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
        Cooldown cooldown = new Cooldown(identifier, key, unit.toMillis(expireTime));
        currentCooldowns.add(cooldown);
        cooldowns.put(identifier, currentCooldowns);

        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), () -> {
            List<Cooldown> updatedCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
            updatedCooldowns.remove(cooldown);
            cooldowns.put(identifier, currentCooldowns);
        }, unit.toSeconds(expireTime) * 20);
    }

    public static void startCooldown(String key, UUID identifier, long expireTime, TimeUnit unit, Consumer<Void> completeCallback) {
        List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
        Cooldown cooldown = new Cooldown(identifier, key, unit.toMillis(expireTime));
        currentCooldowns.add(cooldown);
        cooldowns.put(identifier, currentCooldowns);

        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), () -> {
            List<Cooldown> updatedCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
            updatedCooldowns.remove(cooldown);
            cooldowns.put(identifier, currentCooldowns);
            completeCallback.accept(null);
        }, unit.toSeconds(expireTime) * 20);
    }

    public static void endCooldown(String key, UUID identifier) {
        List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
        currentCooldowns.removeIf(cooldown -> cooldown.getIdentifier().equals(identifier) && cooldown.getCooldownId().equalsIgnoreCase(key));
    }

    public static boolean hasCooldown(String key, UUID identifier) {
        return cooldowns.getOrDefault(identifier, new ArrayList<>()).stream().anyMatch(cooldown -> cooldown.getCooldownId().equalsIgnoreCase(key));
    }

}
