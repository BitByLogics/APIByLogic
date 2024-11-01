package net.bitbylogic.apibylogic.util;

import lombok.NonNull;

import java.util.HashMap;

public class GenericHashMap<K, V> extends HashMap<K, V> {

    public <T> T getValueAsOrDefault(@NonNull K key, T defaultValue) {
        try {
            if (!containsKey(key)) {
                return defaultValue;
            }

            return (T) get(key);
        } catch (ClassCastException exception) {
            return defaultValue;
        }
    }

}
