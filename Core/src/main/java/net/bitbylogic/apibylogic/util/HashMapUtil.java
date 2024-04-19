package net.bitbylogic.apibylogic.util;

import java.util.*;
import java.util.regex.Pattern;

public class HashMapUtil {

    public static <K, V> String mapToString(HashMap<K, V> map, String entrySeparator, String valueSeparator, ObjectParser<K, V> parser) {
        if (map.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        map.forEach((k, v) -> {
            if (parser != null) {
                builder.append(entrySeparator).append(parser.wrapKey(k)).append(valueSeparator).append(parser.wrapValue(v));
            }

            map.keySet().forEach(key -> builder.append(entrySeparator).append(key).append(valueSeparator).append(map.get(key)));
        });

        return builder.substring(1);
    }

    public static String mapToString(HashMap<?, ?> map) {
        return mapToString(map, ";", ":", null);
    }

    public static <K, V> String mapToString(HashMap<K, V> map, ObjectParser<K, V> parser) {
        return mapToString(map, ";", ":", parser);
    }

    public static <K, V> HashMap<K, V> mapFromString(ObjectWrapper<K, V> wrapper, String string) {
        HashMap<K, V> map = new HashMap<>();

        if (string.isEmpty()) {
            return map;
        }

        String[] mapData = string.split(";");

        for (String dataString : mapData) {
            String[] data = dataString.split(":");
            map.put(wrapper == null ? (K) data[0] : wrapper.wrapKey(data[0]), wrapper == null ? (V) data[1] : wrapper.wrapValue(data[1]));
        }

        return map;
    }

    public static <K, V> HashMap<K, V> mapFromString(ObjectWrapper<K, V> wrapper, String entrySeparator, String valueSeparator, String string) {
        HashMap<K, V> map = new HashMap<>();

        if (string.isEmpty()) {
            return map;
        }

        String[] mapData = string.split(Pattern.quote(entrySeparator));

        for (String dataString : mapData) {
            String[] data = dataString.split(Pattern.quote(valueSeparator));
            map.put(wrapper == null ? (K) data[0] : wrapper.wrapKey(data[0]), wrapper == null ? (V) data[1] : wrapper.wrapValue(data[1]));
        }

        return map;
    }

    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new HashSet<T>();
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public interface ObjectWrapper<K, V> {
        K wrapKey(String key);

        V wrapValue(String value);
    }

    public interface ObjectParser<K, V> {
        String wrapKey(K key);

        String wrapValue(V value);
    }

}
