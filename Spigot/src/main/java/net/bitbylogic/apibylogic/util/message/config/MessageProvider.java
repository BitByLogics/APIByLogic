package net.bitbylogic.apibylogic.util.message.config;

import lombok.Getter;
import net.bitbylogic.apibylogic.util.Pair;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.message.Formatter;
import net.bitbylogic.apibylogic.util.message.config.annotation.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class MessageProvider {

    private final HashMap<String, Object> messages = new HashMap<>();
    private final List<StringModifier> placeholders = new ArrayList<>();

    public MessageProvider(ConfigurationSection config) {
        config.getKeys(true).stream().filter(key -> !(config.get(key) instanceof MemorySection)).forEach(key -> messages.put(key, config.get(key)));
    }

    public void loadMessages(FileConfiguration config, @Nullable String messagesPath) {
        messages.clear();

        ConfigurationSection memorySection = config;

        if (messagesPath != null) {
            ConfigurationSection messageSection = config.getConfigurationSection(messagesPath);

            if (messageSection != null) {
                memorySection = messageSection;
            }
        }

        for (String key : memorySection.getKeys(true)) {

        }
    }

    public void reload(ConfigurationSection section) {
        messages.clear();
        section.getKeys(true).stream().filter(key -> !(section.get(key) instanceof MemorySection)).forEach(key -> messages.put(key, section.get(key)));
    }

    @Deprecated
    public void registerPlaceholder(Placeholder placeholder) {
        placeholders.add(placeholder);
    }

    public void registerModifier(StringModifier placeholder) {
        placeholders.add(placeholder);
    }

    public String getMessage(String key, Placeholder... externalPlaceholders) {
        return getMessage(key, true, externalPlaceholders);
    }

    public String getMessage(String key, boolean applyPlaceholders, Placeholder... externalPlaceholders) {
        String rawMessage = getRawMessage(key);

        if (rawMessage == null) {
            return null;
        }

        rawMessage = Formatter.format(rawMessage, externalPlaceholders);

        if (applyPlaceholders) {
            rawMessage = Formatter.format(rawMessage, placeholders.toArray(new StringModifier[]{}));
        }

        return rawMessage;
    }

    public List<String> getMessageList(String key, Placeholder... externalPlaceholders) {
        return getMessageList(key, true, externalPlaceholders);
    }

    public List<String> getMessageList(String key, boolean applyPlaceholders, Placeholder... externalPlaceholders) {
        List<String> rawList = getRawMessageList(key);

        if (rawList == null) {
            return null;
        }

        List<String> formattedMessages = new ArrayList<>();
        rawList.forEach(string -> formattedMessages.add(Formatter.format(string, externalPlaceholders)));

        List<String> finalMessages = new ArrayList<>();

        if (applyPlaceholders) {
            formattedMessages.forEach(string -> finalMessages.add(Formatter.format(string, placeholders.toArray(new StringModifier[]{}))));
        }

        return finalMessages;
    }

    public List<String> getRawMessageList(String key) {
        if (!messages.containsKey(key)) {
            return null;
        }

        if (!(messages.get(key) instanceof List)) {
            return null;
        }

        return (List<String>) messages.get(key);
    }

    public String getRawMessage(String key) {
        if (!messages.containsKey(key)) {
            return null;
        }

        if (!(messages.get(key) instanceof String)) {
            return null;
        }

        return (String) messages.get(key);
    }

    public String applyPlaceholders(String text) {
        return Formatter.format(text, placeholders.toArray(new StringModifier[]{}));
    }

}
