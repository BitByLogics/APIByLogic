package net.bitbylogic.apibylogic.util;

public class EnumUtil {

    public static <T extends Enum<T>> T getValue(Class<T> enumClass, String name, T fallback) {
        for (T constant : enumClass.getEnumConstants()) {
            if (!constant.name().equalsIgnoreCase(name)) {
                continue;
            }

            return constant;
        }

        return fallback;
    }

}
