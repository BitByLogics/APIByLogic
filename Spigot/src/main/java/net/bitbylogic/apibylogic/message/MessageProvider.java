package net.bitbylogic.apibylogic.message;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.bitbylogic.apibylogic.util.Format;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class MessageProvider {

    private final HashMap<String, Object> messages;
    private final List<StringModifier> placeholders;

    public MessageProvider(ConfigurationSection section) {
        messages = Maps.newHashMap();
        placeholders = Lists.newArrayList();

        section.getKeys(true).stream().filter(key -> !(section.get(key) instanceof MemorySection)).forEach(key -> messages.put(key, section.get(key)));
    }

    public MessageProvider(FileConfiguration config) {
        messages = Maps.newHashMap();
        placeholders = Lists.newArrayList();

        config.getKeys(true).stream().filter(key -> !(config.get(key) instanceof MemorySection)).forEach(key -> messages.put(key, config.get(key)));
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

        rawMessage = Format.format(rawMessage, externalPlaceholders);

        if (applyPlaceholders) {
            rawMessage = Format.format(rawMessage, placeholders.toArray(new StringModifier[]{}));
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
        rawList.forEach(string -> formattedMessages.add(Format.format(string, externalPlaceholders)));

        List<String> finalMessages = new ArrayList<>();

        if (applyPlaceholders) {
            formattedMessages.forEach(string -> finalMessages.add(Format.format(string, placeholders.toArray(new StringModifier[]{}))));
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
        return Format.format(text, placeholders.toArray(new StringModifier[]{}));
    }

}
