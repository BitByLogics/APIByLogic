package net.bitbylogic.apibylogic.util;

import java.lang.reflect.Field;

public class ReflectionUtil {

    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        do {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch(Exception e) {}
        } while((currentClass = currentClass.getSuperclass()) != null);

        return null;
    }

}
