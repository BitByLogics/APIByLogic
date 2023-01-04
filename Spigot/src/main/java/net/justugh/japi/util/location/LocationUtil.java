package net.justugh.japi.util.location;

import net.justugh.japi.JustAPIPlugin;
import org.bukkit.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class LocationUtil {

    /**
     * Convert a location to string.
     *
     * @param location The location being converted.
     * @return The converted string.
     */
    public static String locationToString(Location location) {
        return locationToString(location, ":");
    }

    public static String locationToStringWithYawPitch(Location location) {
        return locationToStringWithYawPitch(location, ":");
    }

    public static String locationToString(Location location, String separator) {
        return location.getWorld().getName() + separator +
                location.getBlockX() + separator +
                location.getBlockY() + separator +
                location.getBlockZ();
    }

    public static String locationToStringWithYawPitch(Location location, String separator) {
        return location.getWorld().getName() + separator +
                location.getBlockX() + separator +
                location.getBlockY() + separator +
                location.getBlockZ() + separator +
                location.getYaw() + separator +
                location.getPitch();
    }

    /**
     * Convert a string to a location.
     *
     * @param string The string to convert.
     * @return The converted location.
     */
    public static Location stringToLocation(String string) {
        return stringToLocation(string, ":");
    }

    public static Location stringToLocation(String string, String separator) {
        String[] splitArgs = string.split(separator);

        if (splitArgs.length == 4) {
            return new Location(Bukkit.getWorld(splitArgs[0]), Double.parseDouble(splitArgs[1]), Double.parseDouble(splitArgs[2]), Double.parseDouble(splitArgs[3]));
        } else {
            return new Location(Bukkit.getWorld(splitArgs[0]), Double.parseDouble(splitArgs[1]), Double.parseDouble(splitArgs[2]), Double.parseDouble(splitArgs[3]), Float.parseFloat(splitArgs[4]), Float.parseFloat(splitArgs[5]));
        }
    }

    public static boolean isLocationString(String string) {
        return isLocationString(string, ":");
    }

    public static boolean isLocationString(String string, String separator) {
        return string.split(separator).length >= 4;
    }

    public static boolean matches(Location location, Location other) {
        if (location.getWorld() == null || other.getWorld() == null) {
            return false;
        }

        if (!location.getWorld().getName().equalsIgnoreCase(other.getWorld().getName())) {
            return false;
        }

        return location.getX() == other.getX() && location.getY() == other.getY() && location.getZ() == other.getZ();
    }

    public static HashMap<Location, PersistentDataContainer> getAllPersistentData(Chunk chunk) {
        HashMap<Location, PersistentDataContainer> data = new HashMap<>();
        PersistentDataContainer dataContainer = chunk.getPersistentDataContainer();

        if (dataContainer.isEmpty()) {
            return data;
        }

        dataContainer.getKeys().forEach(key -> {
            if (!isLocationString(key.getKey(), "._.")) {
                return;
            }

            if (dataContainer.get(key, PersistentDataType.TAG_CONTAINER) == null) {
                return;
            }

            data.put(stringToLocation(key.getKey(), "._."), dataContainer.get(key, PersistentDataType.TAG_CONTAINER));
        });

        return data;
    }

    public static boolean hasPersistentData(Location location) {
        return location.getChunk().getPersistentDataContainer().has(new NamespacedKey(JustAPIPlugin.getInstance(), locationToString(location, "._.")), PersistentDataType.TAG_CONTAINER);
    }

    public static PersistentDataContainer getPersistentData(Location location, boolean create) {
        NamespacedKey locationKey = new NamespacedKey(JustAPIPlugin.getInstance(), locationToString(location, "._."));
        Chunk chunk = location.getChunk();

        if (!chunk.getPersistentDataContainer().has(locationKey, PersistentDataType.TAG_CONTAINER)) {
            if (!create) {
                return null;
            }

            chunk.getPersistentDataContainer().set(locationKey, PersistentDataType.TAG_CONTAINER,
                    chunk.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer());
        }

        return chunk.getPersistentDataContainer().get(locationKey, PersistentDataType.TAG_CONTAINER);
    }

    public static void deletePersistentData(Location location) {
        NamespacedKey locationKey = new NamespacedKey(JustAPIPlugin.getInstance(), locationToString(location, "._."));
        Chunk chunk = location.getChunk();

        if (!chunk.getPersistentDataContainer().has(locationKey, PersistentDataType.TAG_CONTAINER)) {
            return;
        }

        chunk.getPersistentDataContainer().remove(locationKey);
    }

    public static void savePersistentData(Location location, PersistentDataContainer container) {
        location.getChunk().getPersistentDataContainer().set(new NamespacedKey(JustAPIPlugin.getInstance(), locationToString(location, "._.")), PersistentDataType.TAG_CONTAINER, container);
    }

    public static int getHighestBlockY(World world, int x, int z) {
        int currentY = world.getMaxHeight();
        int minY = world.getMinHeight();

        world.getChunkAt(x, z);

        for (int y = currentY; y > minY; y--) {
            if (world.getBlockAt(x, y, z).getType().isAir()) {
                continue;
            }

            return y;
        }

        return 0;
    }

}
