package net.bitbylogic.apibylogic.util.location;

import lombok.NonNull;
import net.bitbylogic.apibylogic.util.config.ConfigParser;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public class OffsetLocationConfigParser implements ConfigParser<OffsetLocation> {

    @Override
    public Optional<OffsetLocation> parseFrom(@NonNull ConfigurationSection section) {
        double xOffset = section.getDouble("X-Offset");
        double yOffset = section.getDouble("Y-Offset");
        double zOffset = section.getDouble("Z-Offset");

        return Optional.of(new OffsetLocation(xOffset, yOffset, zOffset));
    }

    @Override
    public ConfigurationSection parseTo(@NonNull ConfigurationSection section, @NonNull OffsetLocation location) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
