package net.bitbylogic.apibylogic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ListUtil {

    public static String listToString(List<?> list, String separator) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (Object o : list) {
            builder.append(separator).append(o.toString());
        }

        return builder.toString().replaceFirst(Pattern.quote(separator), "");
    }

    public static String listToString(List<?> list) {
        return listToString(list, ":");
    }


    public static <V> List<V> stringToList(String string, String separator) {
        return stringToList(string, separator, null);
    }

    public static <V> List<V> stringToList(String string) {
        return stringToList(string, ":");
    }

    public static <V> List<V> stringToList(String string, String separator, ListObjectWrapper<V> wrapper) {
        List<V> list = new ArrayList<>();

        if (string == null || string.isEmpty()) {
            return list;
        }

        for (String value : string.split(separator)) {
            list.add(wrapper == null ? (V) value : wrapper.wrapValue(value));
        }

        return list;
    }

    public interface ListObjectWrapper<V> {
        V wrapValue(String string);
    }

}
