package net.bitbylogic.apibylogic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class ListUtil {

    public static String listToString(List<?> list) {
        return listToString(list, ":", null);
    }

    public static <O> String listToString(List<O> list, ListObjectWrapper<String, O> wrapper) {
        return listToString(list, ":", wrapper);
    }

    public static <O> String listToString(List<O> list, String separator, ListObjectWrapper<String, O> wrapper) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(separator);

        for (O o : list) {
            joiner.add(wrapper == null ? o.toString() : wrapper.wrapValue(o));
        }

        return joiner.toString();
    }

    public static List<?> stringToList(String string, String separator) {
        return stringToList(string, separator, null);
    }

    public static List<?> stringToList(String string) {
        return stringToList(string, ":");
    }

    public static <V> List<V> stringToList(String string, String separator, ListObjectWrapper<V, String> wrapper) {
        List<V> list = new ArrayList<>();

        if (string == null || string.isEmpty()) {
            return list;
        }

        for (String value : string.split(separator)) {
            list.add(wrapper == null ? (V) value : wrapper.wrapValue(value));
        }

        return list;
    }

    public interface ListObjectWrapper<V, K> {
        V wrapValue(K object);
    }

}
