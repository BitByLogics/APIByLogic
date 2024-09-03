package net.bitbylogic.apibylogic.util.config;

import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.util.config.annotation.ConfigPath;
import net.bitbylogic.apibylogic.util.config.data.ConfigFieldData;
import net.bitbylogic.apibylogic.util.config.wrapper.LogicConfigWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class Configurable {

    private final HashMap<String, LogicConfigWrapper> valueWrappers = new HashMap<>();

    @Setter
    private File configFile;

    @Setter
    private String globalPrefix;

    public Configurable(@NonNull File configFile, @NonNull String globalPrefix) {
        this.configFile = configFile;
        this.globalPrefix = globalPrefix;

        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), this::loadConfigPaths, 1);
    }

    public Configurable(@NonNull File configFile) {
        this.configFile = configFile;

        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), this::loadConfigPaths, 1);
    }

    public Configurable() {
        this.configFile = null;

        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), this::loadConfigPaths, 1);
    }

    public void loadConfigPaths() {
        if (configFile == null) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        getFieldData(fieldData -> {
            for (ConfigFieldData data : fieldData) {
                Field field = data.getField();
                String fieldPath = data.getFieldPath();
                LogicConfigWrapper wrapper = data.getWrapper();

                try {
                    Object value = field.get(this);

                    if (!config.isSet(fieldPath)) {
                        config.set(fieldPath, wrapper == null ? value : wrapper.wrap(value));
                        continue;
                    }

                    Object configValue = config.get(fieldPath);
                    field.set(this, wrapper == null ? configValue : wrapper.unwrap(configValue));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveToConfig() {
        if (configFile == null) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        getFieldData(fieldData -> {
            for (ConfigFieldData data : fieldData) {
                Field field = data.getField();
                String fieldPath = data.getFieldPath();
                LogicConfigWrapper wrapper = data.getWrapper();

                try {
                    Object value = field.get(this);
                    config.set(fieldPath, wrapper == null ? value : wrapper.wrap(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void getFieldData(Consumer<List<ConfigFieldData>> consumer) {
        HashMap<Field, Boolean> accessibleInfo = new HashMap<>();
        List<ConfigFieldData> fieldData = new ArrayList<>();

        Class<?> clazz = this.getClass();

        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(ConfigPath.class)) {
                    continue;
                }

                boolean accessible = field.isAccessible();

                if (!accessible) {
                    field.setAccessible(true);
                }

                accessibleInfo.put(field, accessible);

                ConfigPath valueSettings = field.getAnnotation(ConfigPath.class);

                String path = valueSettings.path().isEmpty() ? field.getType().getName() : valueSettings.path();
                String fieldPath = globalPrefix == null ? path : globalPrefix + path;
                LogicConfigWrapper wrapper = valueSettings.wrapperId().isEmpty() ? null
                        : valueWrappers.getOrDefault(valueSettings.wrapperId(), null);

                fieldData.add(new ConfigFieldData(field, fieldPath, wrapper));
            }

            clazz = clazz.getSuperclass();
        } while (clazz != null);

        consumer.accept(fieldData);

        accessibleInfo.forEach(Field::setAccessible);
    }

    public void registerWrapper(String id, LogicConfigWrapper wrapper) {
        valueWrappers.put(id, wrapper);
    }

}
