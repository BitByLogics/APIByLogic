package net.bitbylogic.apibylogic.util.location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class SafeLocation implements Serializable {

    private final String world;
    private final double x, y, z;

    public static SafeLocation fromString(String string) {
        String[] data = string.split(":");
        return new SafeLocation(data[0], Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3]));
    }

    public static SafeLocation fromBukkitLocation(Location location) {
        return new SafeLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public boolean matches(Location loc) {
        return loc.getWorld().getName().equalsIgnoreCase(world) && loc.getX() == x && loc.getY() == y && loc.getZ() == z;
    }

    public Location toBukkitLocation() {
        if (Bukkit.getWorld(world) == null) {
            return null;
        }

        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s:%s", world, x, y, z);
    }
}
