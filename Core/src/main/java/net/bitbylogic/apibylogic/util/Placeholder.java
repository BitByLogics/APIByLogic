package net.bitbylogic.apibylogic.util;

import lombok.Getter;

import java.util.HashMap;

@Getter
public class Placeholder implements StringModifier {

    private final HashMap<String, String> placeholderMap = new HashMap<>();

    public Placeholder(String key, Object value) {
        placeholderMap.put(key, value instanceof String ? (String) value : value.toString());
    }

    public Placeholder(String key, String value) {
        placeholderMap.put(key, value);
    }

    public Placeholder(String... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 >= replacements.length) {
                placeholderMap.put(replacements[i], "Invalid Placeholder");
                break;
            }

            placeholderMap.put(replacements[i], replacements[i + 1]);
        }
    }

    @Override
    public String modify(String string) {
        String modifiedString = string;

        for (String key : placeholderMap.keySet()) {
            modifiedString = modifiedString.replace(key, placeholderMap.get(key));
        }

        return modifiedString;
    }

}
