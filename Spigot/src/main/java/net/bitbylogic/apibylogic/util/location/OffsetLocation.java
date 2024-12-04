package net.bitbylogic.apibylogic.util.location;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

import java.util.Objects;

@RequiredArgsConstructor
@Getter
public class OffsetLocation {

    private final double xOffset;
    private final double yOffset;
    private final double zOffset;

    public static OffsetLocation fromString(@NonNull String string) {
        String[] data = string.split(":");

        if (data.length < 3) {
            return new OffsetLocation(0, 0, 0);
        }

        return new OffsetLocation(Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]));
    }

    public static @NonNull OffsetLocation of(@NonNull Location location) {
        return new OffsetLocation(location.getX(), location.getY(), location.getZ());
    }

    public static OffsetLocationConfigParser getParser() {
        return new OffsetLocationConfigParser();
    }

    public @NonNull Location apply(@NonNull Location location) {
        return location.clone().add(xOffset, yOffset, zOffset);
    }

    public boolean matches(@NonNull Location location, @NonNull Location center) {
        return LocationUtil.matches(apply(center.clone()), LocationUtil.toBlockLocation(location));
    }

    public double distance(@NonNull OffsetLocation location) {
        double dx = location.getXOffset() - xOffset;
        double dy = location.getYOffset() - yOffset;
        double dz = location.getZOffset() - zOffset;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean isAdjacent(@NonNull OffsetLocation location) {
        return (xOffset != location.getXOffset() ? 1 : 0) +
                (yOffset != location.getYOffset() ? 1 : 0) +
                (zOffset != location.getZOffset() ? 1 : 0) == 1;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        OffsetLocation that = (OffsetLocation) object;
        return Double.compare(xOffset, that.xOffset) == 0 && Double.compare(yOffset, that.yOffset) == 0 && Double.compare(zOffset, that.zOffset) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xOffset, yOffset, zOffset);
    }

    @Override
    public String toString() {
        return String.format("%.2f:%.2f:%.2f", xOffset, yOffset, zOffset);
    }

}
