package net.bitbylogic.apibylogic.util.config.wrapper.impl;

import lombok.NonNull;
import net.bitbylogic.apibylogic.util.config.wrapper.LogicConfigWrapper;
import net.bitbylogic.apibylogic.util.item.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultConfigWrapper implements LogicConfigWrapper<Object> {

    @Override
    public void wrap(@NonNull Object object, @NonNull String path, @NonNull FileConfiguration config) {
        if (object instanceof ItemStack) {
            ConfigurationSection section = config.isConfigurationSection(path) ? config.getConfigurationSection(path) : config.createSection(path);
            ItemStackUtil.saveItemStackToConfiguration((ItemStack) object, section);
            return;
        }

        if (object instanceof HashMap<?,?>) {
            ((HashMap<?, ?>) object).forEach((key, value) -> {
                if(!(key instanceof String) || !(value instanceof ItemStack)) {
                    return;
                }

                ConfigurationSection section = config.isConfigurationSection(path + key) ? config.getConfigurationSection(path + key)
                        : config.createSection(path + key);
                ItemStackUtil.saveItemStackToConfiguration((ItemStack) value, section);
            });
            return;
        }

        config.set(path, object);
    }

    @Override
    public <W> Object unwrap(@NonNull W wrappedObject, @NonNull Class<?> requestedClass) {
        if (requestedClass.isAssignableFrom(ItemStack.class) && wrappedObject instanceof ConfigurationSection) {
            return ItemStackUtil.getItemStackFromConfig((ConfigurationSection) wrappedObject);
        }

        if(requestedClass.isAssignableFrom(Map.class) && wrappedObject instanceof ConfigurationSection) {
            try {
                Map<String, ItemStack> map = (Map<String, ItemStack>) requestedClass.newInstance();

                for (String key : ((ConfigurationSection) wrappedObject).getKeys(false)) {
                    map.put(key, ItemStackUtil.getItemStackFromConfig(((ConfigurationSection) wrappedObject).getConfigurationSection(key)));
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
