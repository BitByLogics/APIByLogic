package net.bitbylogic.apibylogic.config;

import net.bitbylogic.apibylogic.util.message.config.annotation.ConfigValue;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

public class LogicConfig {

    private final File configFile;
    private final HashMap<String, LogicConfigWrapper> valueWrappers;

    public LogicConfig(File configFile) {
        this.configFile = configFile;
        this.valueWrappers = new HashMap<>();

        loadConfigData();
    }

    public void loadConfigData() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Class<?> clazz = this.getClass();

        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(ConfigValue.class)) {
                    continue;
                }

                boolean accessible = field.isAccessible();

                if (!accessible) {
                    field.setAccessible(true);
                }

                ConfigValue valueSettings = field.getAnnotation(ConfigValue.class);

                String fieldPath = valueSettings.path().isEmpty() ? field.getType().getName() : valueSettings.path();
                LogicConfigWrapper wrapper = valueSettings.wrapperId().isEmpty() ? null
                        : valueWrappers.getOrDefault(valueSettings.wrapperId(), null);

                try {
                    Object value = field.get(this);

                    if (!config.isSet(fieldPath)) {
                        config.set(fieldPath, wrapper == null ? value : wrapper.wrap(value));
                        continue;
                    }

                    Object configValue = config.get(fieldPath);

                    field.set(this, wrapper == null ? configValue : wrapper.unwrap(configValue));
                    field.setAccessible(accessible);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            clazz = clazz.getSuperclass();
        } while (clazz != null);

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
