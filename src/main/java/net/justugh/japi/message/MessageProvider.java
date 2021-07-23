package net.justugh.japi.message;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.justugh.japi.util.Format;
import net.justugh.japi.util.Placeholder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;

@Getter
public class MessageProvider {

    private final HashMap<String, Object> messages;
    private final List<Placeholder> placeholders;

    public MessageProvider(ConfigurationSection section) {
        messages = Maps.newHashMap();
        placeholders = Lists.newArrayList();

        section.getKeys(true).forEach(key -> messages.put(key, section.get(key)));
    }

    public void registerPlaceholder(Placeholder placeholder) {
        placeholders.add(placeholder);
    }

    public String getMessage(String key, Placeholder... externalPlaceholders) {
        return getMessage(key, false, externalPlaceholders);
    }

    public String getMessage(String key, boolean applyPlaceholders, Placeholder... externalPlaceholders) {
        String rawMessage = getRawMessage(key);

        if (rawMessage == null) {
            return null;
        }

        if (applyPlaceholders) {
            rawMessage = Format.format(rawMessage, placeholders.toArray(new Placeholder[]{}));
        }

        return Format.format(rawMessage, externalPlaceholders);
    }

    public List<String> getMessageList(String key, Placeholder... externalPlaceholders) {
        return getMessageList(key, false, externalPlaceholders);
    }

    public List<String> getMessageList(String key, boolean applyPlaceholders, Placeholder... externalPlaceholders) {
        List<String> rawList = getRawMessageList(key);

        if (rawList == null) {
            return null;
        }

        List<String> formattedMessages = Lists.newArrayList();

        if (applyPlaceholders) {
            Placeholder[] placeholderArray = placeholders.toArray(new Placeholder[]{});
            rawList.forEach(string -> formattedMessages.add(Format.format(string, placeholderArray)));
        }

        List<String> finalMessages = Lists.newArrayList();

        if (formattedMessages.isEmpty()) {
            rawList.forEach(string -> finalMessages.add(Format.format(string, externalPlaceholders)));
        } else {
            formattedMessages.forEach(string -> finalMessages.add(Format.format(string, externalPlaceholders)));
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

}
