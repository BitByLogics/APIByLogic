package net.bitbylogic.apibylogic.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StringProcessor {

    DEFAULT(new Class[]{String.class}, (tc, s) -> s),
    INT(new Class[]{Integer.class, int.class}, (tc, s) -> {
        Integer value = Integer.parseInt(s);
        return tc.isPrimitive() ? value.intValue() : value;
    }),
    LONG(new Class[]{Long.class, long.class}, (tc, s) -> {
        Long value = Long.parseLong(s);
        return tc.isPrimitive() ? value.longValue() : value;
    }),
    DOUBLE(new Class[]{Double.class, double.class}, (tc, s) -> {
        Double value = Double.parseDouble(s);
        return tc.isPrimitive() ? value.doubleValue() : value;
    }),
    FLOAT(new Class[]{Float.class, float.class}, (tc, s) -> {
        Float value = Float.parseFloat(s);
        return tc.isPrimitive() ? value.floatValue() : value;
    }),
    SHORT(new Class[]{Short.class, short.class}, (tc, s) -> {
        Short value = Short.parseShort(s);
        return tc.isPrimitive() ? value.shortValue() : value;
    }),
    BYTE(new Class[]{Byte.class, byte.class}, (tc, s) -> {
        Byte value = Byte.parseByte(s);
        return tc.isPrimitive() ? value.byteValue() : value;
    }),
    BOOLEAN(new Class[]{Boolean.class, boolean.class}, (tc, s) -> {
        Boolean value = Boolean.parseBoolean(s);
        return tc.isPrimitive() ? value.booleanValue() : value;
    }),
    CHAR(new Class[]{Character.class, char.class}, (tc, s) -> s.isEmpty() ? 'A' : s.charAt(0)),
    UUID(new Class[]{java.util.UUID.class}, (tc, s) -> java.util.UUID.fromString(s)),
    ENUM(new Class[]{Enum.class}, (tc, s) -> EnumUtil.getValue((Class<Enum>) tc, s, null));

    private final Class<?>[] dataTypes;
    private final StringProcessorFunction<?> processor;

    public static Object findAndProcess(Class<?> targetClass, String value) {
        for (StringProcessor internalProcessor : values()) {
            for (Class<?> dataType : internalProcessor.getDataTypes()) {
                if (!dataType.equals(targetClass)) {
                    continue;
                }

                return internalProcessor.getProcessor().process(targetClass, value);
            }
        }

        return value;
    }

    private interface StringProcessorFunction<V> {

        V process(Class<?> targetClass, String value);

    }

}
