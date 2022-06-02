package net.justugh.japi.util;

import java.util.HashMap;

public class HashMapUtil {

    public static String mapToString(HashMap<?, ?> map) {
        if (map.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        map.keySet().forEach(key -> builder.append(";").append(key).append(":").append(map.get(key)));
        return builder.substring(1);
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

    public interface ObjectWrapper<K, V> {
        K wrapKey(String key);

        V wrapValue(String value);
    }

}
