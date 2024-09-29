package net.bitbylogic.apibylogic.util;

import lombok.NonNull;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.util.cooldown.Cooldown;
import org.bukkit.Bukkit;

import java.util.*;
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
            cooldowns.put(identifier, updatedCooldowns);
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
            cooldowns.put(identifier, updatedCooldowns);
            completeCallback.accept(null);
        }, unit.toSeconds(expireTime) * 20);
    }

    public static void attemptRun(String key, UUID identifier, String cooldownTime, Runnable runnable) {
        if (!hasCooldown(key, identifier)) {
            List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
            Cooldown cooldown = new Cooldown(identifier, key, TimeConverter.convert(cooldownTime));
            currentCooldowns.add(cooldown);
            cooldowns.put(identifier, currentCooldowns);
            runnable.run();
            return;
        }

        getCooldown(key, identifier).ifPresent(cooldown -> {
            if (cooldown.isActive()) {
                return;
            }

            List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
            Cooldown newCooldown = new Cooldown(identifier, key, TimeConverter.convert(cooldownTime));
            currentCooldowns.remove(cooldown);
            currentCooldowns.add(newCooldown);
            cooldowns.put(identifier, currentCooldowns);
            runnable.run();
        });
    }

    public static void endCooldown(String key, UUID identifier) {
        List<Cooldown> currentCooldowns = cooldowns.getOrDefault(identifier, new ArrayList<>());
        currentCooldowns.removeIf(cooldown -> cooldown.getIdentifier().equals(identifier) && cooldown.getCooldownId().equalsIgnoreCase(key));
    }

    public static Optional<Cooldown> getCooldown(@NonNull String key, @NonNull UUID identifier) {
        return cooldowns.getOrDefault(identifier, new ArrayList<>()).stream().filter(cd -> cd.getCooldownId().equalsIgnoreCase(key)).findFirst();
    }

    public static boolean hasCooldown(String key, UUID identifier) {
        return getCooldown(key, identifier).isPresent();
    }

    public static int getRemainingTime(@NonNull String key, @NonNull UUID identifier) {
        if (!hasCooldown(key, identifier)) {
            return -1;
        }

        return getCooldown(key, identifier).map(cooldown -> (int) (cooldown.getTimeUntilExpired() / 1000)).orElse(-1);
    }

}
