package net.bitbylogic.apibylogic.util.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public class ConfigHelper {

    private final @Nullable FileConfiguration config;
    private final @Nullable ConfigurationSection section;

    public ConfigHelper(@NonNull FileConfiguration config) {
        this.config = config;
        this.section = null;
    }

    public ConfigHelper(@NonNull ConfigurationSection section) {
        this.config = null;
        this.section = section;
    }

    public void setIfMissing(@NonNull String path, @NonNull Object value) {
        if(config != null && !config.contains(path)) {
            config.set(path, value);
        }

        if(section == null || section.contains(path)) {
            return;
        }

        section.set(path, value);
    }

}
