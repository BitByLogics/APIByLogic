package net.bitbylogic.apibylogic.util.config.configurable.wrapper.impl;

import lombok.NonNull;
import net.bitbylogic.apibylogic.util.config.configurable.wrapper.LogicConfigWrapper;
import net.bitbylogic.apibylogic.util.item.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DefaultConfigWrapper implements LogicConfigWrapper<Object> {

    @Override
    public void wrap(@NonNull Object object, @NonNull String path, @NonNull FileConfiguration config) {
        if (object instanceof ItemStack) {
            ConfigurationSection section = config.getConfigurationSection(path) == null ? config.getConfigurationSection(path) : config.createSection(path);

            if(section == null) {
                return;
            }

            ItemStackUtil.saveToConfig(section, (ItemStack) object);
            return;
        }

        if (object instanceof HashMap<?,?>) {
            ((HashMap<?, ?>) object).forEach((key, value) -> {
                if(!(key instanceof String) || !(value instanceof ItemStack)) {
                    return;
                }

                ConfigurationSection section = config.isConfigurationSection(path + key) ? config.getConfigurationSection(path + key)
                        : config.createSection(path + key);

                if(section == null) {
                    return;
                }

                ItemStackUtil.saveToConfig(section, (ItemStack) value);
            });

            return;
        }

        config.set(path, object);
    }

    @Override
    public <W> Object unwrap(@NonNull W wrappedObject, @NonNull Class<?> requestedClass) {
        if (requestedClass.isAssignableFrom(ItemStack.class) && wrappedObject instanceof ConfigurationSection) {
            return ItemStackUtil.getFromConfig((ConfigurationSection) wrappedObject);
        }

        if(requestedClass.isAssignableFrom(Map.class) && wrappedObject instanceof ConfigurationSection) {
            try {
                Map<String, ItemStack> map = (Map<String, ItemStack>) requestedClass.newInstance();

                for (String key : ((ConfigurationSection) wrappedObject).getKeys(false)) {
                    map.put(key, ItemStackUtil.getFromConfig(((ConfigurationSection) wrappedObject).getConfigurationSection(key)));
                }

                return map;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return null;
        }

        return wrappedObject;
    }
}
