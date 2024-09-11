package net.bitbylogic.apibylogic.util.config;

import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.util.Pair;
import net.bitbylogic.apibylogic.util.config.data.ConfigKeyData;
import net.bitbylogic.apibylogic.util.config.wrapper.LogicConfigWrapper;
import net.bitbylogic.apibylogic.util.config.wrapper.impl.DefaultConfigWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

public class Configurable {

    private final DefaultConfigWrapper DEFAULT_WRAPPER = new DefaultConfigWrapper();

    private final HashMap<ConfigKeyData, Object> configValues = new HashMap<>();
    private final HashMap<String, LogicConfigWrapper<?>> valueWrappers = new HashMap<>();

    @Setter
    private File configFile;

    @Setter
    private String globalPrefix;


    public Configurable(@NonNull File configFile, @NonNull String globalPrefix, @NonNull Pair<Object, Object>... defaultValues) {
        this.configFile = configFile;
        this.globalPrefix = globalPrefix;

        for (Pair<Object, Object> defaultValue : defaultValues) {
            Object key = defaultValue.getKey();
            Object value = defaultValue.getValue();

            ConfigKeyData configKeyData = key instanceof ConfigKeyData ? (ConfigKeyData) key : ConfigKeyData.of(key.toString());
            configValues.put(configKeyData, value);
        }

        loadConfigPaths();
    }

    public Configurable(@NonNull File configFile, @NonNull String globalPrefix) {
        this.configFile = configFile;
        this.globalPrefix = globalPrefix;

        loadConfigPaths();
    }

    public Configurable(@NonNull File configFile, @NonNull Pair<Object, Object>... defaultValues) {
        this.configFile = configFile;

        for (Pair<Object, Object> defaultValue : defaultValues) {
            Object key = defaultValue.getKey();
            Object value = defaultValue.getValue();

            ConfigKeyData configKeyData = key instanceof ConfigKeyData ? (ConfigKeyData) key : ConfigKeyData.of(key.toString());
            configValues.put(configKeyData, value);
        }

        loadConfigPaths();
    }

    public Configurable(@NonNull File configFile) {
        this.configFile = configFile;

        loadConfigPaths();
    }

    public Configurable() {
        this.configFile = null;

        loadConfigPaths();
    }

    protected static Pair<Object, Object> pair(Object key, Object value) {
        return new Pair<>(key, value);
    }

    public void loadConfigPaths() {
        if (configFile == null || configValues.isEmpty()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        configValues.forEach((data, object) -> {
            String path = data.getPath();
            String wrapperId = data.getWrapperId();

            String finalPath = globalPrefix == null ? path : globalPrefix + path;
            LogicConfigWrapper wrapper = wrapperId.isEmpty() ? DEFAULT_WRAPPER
                    : valueWrappers.getOrDefault(wrapperId, DEFAULT_WRAPPER);

            if (!config.isSet(path)) {
                if (wrapper == null) {
                    config.set(finalPath, object);
                    return;
                }

                wrapper.wrap(object, finalPath, config);
                return;
            }

            Object configValue = config.get(finalPath);

            if (configValue == null) {
                return;
            }

            configValues.put(data, wrapper == null ? configValue : wrapper.unwrap(configValue, object.getClass()));
        });

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToConfig() {
        if (configFile == null || configValues.isEmpty()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        configValues.forEach((data, object) -> {
            String path = data.getPath();
            String wrapperId = data.getWrapperId();

            String finalPath = globalPrefix == null ? path : globalPrefix + path;
            LogicConfigWrapper wrapper = wrapperId.isEmpty() ? DEFAULT_WRAPPER
                    : valueWrappers.getOrDefault(wrapperId, DEFAULT_WRAPPER);

            if (wrapper == null) {
                config.set(finalPath, object);
                return;
            }

            wrapper.wrap(object, finalPath, config);
        });
    }

    public boolean hasConfigValue(@NonNull String path) {
        return configValues.entrySet().stream().anyMatch(entry -> entry.getKey().getPath().equalsIgnoreCase(path));
    }

    public <T> T getConfigValue(@NonNull String path) {
        return configValues.entrySet().stream()
                .filter(entry -> entry.getKey().getPath().equalsIgnoreCase(path))
                .map(entry -> (T) entry.getValue()).findFirst().orElse(null);
    }

    public <T> T getConfigValueOrDefault(@NonNull String path, @NonNull T defaultValue) {
        return getConfigValueOrDefault(path, defaultValue, true);
    }

    public <T> T getConfigValueOrDefault(@NonNull String path, @NonNull T defaultValue, boolean save) {
        if (configFile == null) {
            return defaultValue;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (hasConfigValue(path)) {
            try {
                return (T) getConfigValue(path);
            } catch (ClassCastException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Unable to cast config value");
                e.printStackTrace();
            }

            return defaultValue;
        }

        Object actualValue = config.get(path);

        if (actualValue == null && save) {
            String finalPath = globalPrefix == null ? path : globalPrefix + path;

            config.set(finalPath, defaultValue);
            configValues.put(ConfigKeyData.of(finalPath, ""), defaultValue);
            saveConfig();
        }

        try {
            return actualValue == null ? defaultValue : (T) actualValue;
        } catch (ClassCastException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Unable to cast config value");
            e.printStackTrace();
        }

        return defaultValue;
    }

    private void saveConfig() {
        if (configFile == null) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerWrapper(String id, LogicConfigWrapper wrapper) {
        valueWrappers.put(id, wrapper);
    }

}
