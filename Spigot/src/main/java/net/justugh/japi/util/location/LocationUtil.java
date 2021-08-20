package net.justugh.japi.util.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationUtil {

    /**
     * Convert a location to string.
     *
     * @param location The location being converted.
     * @return The converted string.
     */
    public static String locationToString(Location location) {
        return location.getWorld().getName() + ":" +
                location.getX() + ":" +
                location.getY() + ":" +
                location.getZ() + ":" +
                location.getYaw() + ":" +
                location.getPitch();
    }

    /**
     * Convert a string to a location.
     *
     * @param string The string to convert.
     * @return The converted location.
     */
    public static Location stringToLocation(String string) {
        String[] splitArgs = string.split(":");

        if (splitArgs.length == 4) {
            return new Location(Bukkit.getWorld(splitArgs[0]), Double.parseDouble(splitArgs[1]), Double.parseDouble(splitArgs[2]), Double.parseDouble(splitArgs[3]));
        } else {
            return new Location(Bukkit.getWorld(splitArgs[0]), Double.parseDouble(splitArgs[1]), Double.parseDouble(splitArgs[2]), Double.parseDouble(splitArgs[3]), Float.parseFloat(splitArgs[4]), Float.parseFloat(splitArgs[5]));
        }
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

}
