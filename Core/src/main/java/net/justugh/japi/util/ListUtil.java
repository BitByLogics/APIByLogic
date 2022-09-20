package net.justugh.japi.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {

    public static String listToString(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Object o : list) {
            builder.append(":").append(o.toString());
        }

        return builder.toString().replaceFirst(":", "");
    }

    public static <V> List<V> stringToList(String string) {
        return stringToList(string, null);
    }

    public static <V> List<V> stringToList(String string, ListObjectWrapper<V> wrapper) {
        List<V> list = new ArrayList<>();

        if (string == null || string.isEmpty()) {
            return list;
        }

        for (String value : string.split(":")) {
            list.add(wrapper == null ? (V) value : wrapper.wrapValue(value));
        }

        return list;
    }

    public interface ListObjectWrapper<V> {
        V wrapValue(String string);
    }

}
