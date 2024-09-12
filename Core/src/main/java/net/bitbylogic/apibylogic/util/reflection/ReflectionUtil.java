package net.bitbylogic.apibylogic.util.reflection;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtil {

    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        do {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (Exception e) {
            }
        } while ((currentClass = currentClass.getSuperclass()) != null);

        return null;
    }

    public static boolean isListOf(Field field, Class<?> entryClass) {
        field.setAccessible(true);
        Type fieldType = field.getGenericType();

        if (fieldType instanceof ParameterizedType parameterizedType) {
            Type[] arguments = parameterizedType.getActualTypeArguments();

            if (arguments.length > 0) {
                return arguments[0].getClass().isInstance(entryClass);
            }
        }

        return false;
    }

    public static boolean isMapOf(Field field, Class<?> valueClass) {
        field.setAccessible(true);
        Type fieldType = field.getGenericType();

        if (fieldType instanceof ParameterizedType parameterizedType) {
            Type[] arguments = parameterizedType.getActualTypeArguments();

            if (arguments.length > 1) {
                return arguments[1].getClass().isInstance(valueClass);
            }
        }

        return false;
    }

    public static Class<?> getParameterizedType(Field field, Class<?> valueClass) {
        field.setAccessible(true);

        if (isListOf(field, valueClass)) {
            Type fieldType = field.getGenericType();

            if (fieldType instanceof ParameterizedType parameterizedType) {
                Type[] arguments = parameterizedType.getActualTypeArguments();

                if (arguments.length > 0) {
                    return arguments[0].getClass().isInstance(valueClass) ? arguments[0].getClass() : null;
                }
            }

            return null;
        }

        if (isMapOf(field, valueClass)) {
            Type fieldType = field.getGenericType();

            if (fieldType instanceof ParameterizedType parameterizedType) {
                Type[] arguments = parameterizedType.getActualTypeArguments();

                if (arguments.length > 1) {
                    return arguments[1].getClass().isInstance(valueClass) ? arguments[1].getClass() : null;
                }
            }

            return null;
        }

        return field.getType();
    }

    public static void createAndPopulateField(Object targetObject, Field field, Object[] values) throws IllegalAccessException {
        field.setAccessible(true);
        Type fieldType = field.getGenericType();

        if (fieldType instanceof ParameterizedType parameterizedType) {
            Class<?> fieldClass = (Class<?>) parameterizedType.getRawType();

            if (List.class.isAssignableFrom(fieldClass)) {
                List<Object> list = new ArrayList<>(Arrays.asList(values));
                field.set(targetObject, list);
            }

            if (!Map.class.isAssignableFrom(fieldClass)) {
                return;
            }

            Map<Object, Object> map = new HashMap<>();

            for (int i = 0; i < values.length; i += 2) {
                if (i + 1 < values.length) {
                    map.put(values[i], values[i + 1]);
                }
            }

            field.set(targetObject, map);
        }
    }

    public static <T> Constructor<T> findNamedConstructor(Class<T> clazz, NamedParameter... parameters) {
        List<Class<?>> parameterClasses = new ArrayList<>();

        for (NamedParameter parameter : parameters) {
            parameterClasses.add(parameter.getValueClass());
        }

        return findConstructor(clazz, parameterClasses.toArray(new Class<?>[]{}));
    }

    public static <T> Constructor<T> findConstructor(Class<T> clazz, Object... parameters) {
        LinkedList<Class<?>> parameterClasses = new LinkedList<>();

        for (Object parameter : parameters) {
            parameterClasses.add(parameter.getClass());
        }

        return findConstructor(clazz, parameterClasses.toArray(new Class<?>[]{}));
    }

    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... parameters) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {

            if (constructor.getParameterTypes().length != parameters.length) {
                continue;
            }

            if (!new HashSet<>(List.of(constructor.getParameterTypes())).containsAll(List.of(parameters))) {
                continue;
            }

            return (Constructor<T>) constructor;
        }

        return null;
    }

    public static <T> T callConstructor(Constructor<T> constructor, NamedParameter... parameters) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        HashMap<String, NamedParameter> parameterMap = new HashMap<>();

        for (NamedParameter parameter : parameters) {
            parameterMap.put(parameter.getName(), parameter);
        }

        Parameter[] constructorParameters = constructor.getParameters();
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] orderedParameters = new Object[parameters.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameter = constructorParameters[i];
            NamedParameter namedParameter = parameterMap.get(parameter.getName());

            if (namedParameter == null) {
                throw new NullPointerException("Unable to find named parameter for parameter: " + parameter.getName());
            }

            orderedParameters[i] = namedParameter.getValue();
        }

        return constructor.newInstance(orderedParameters);
    }

    public static <T> T callConstructor(Constructor<T> constructor, Object... parameters) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        HashMap<Class<?>, Object> parameterMap = new HashMap<>();

        for (Object parameter : parameters) {
            parameterMap.put(parameter.getClass(), parameter);
        }

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] orderedParameters = new Object[parameters.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            orderedParameters[i] = parameterMap.get(parameterTypes[i]);
        }

        return constructor.newInstance(orderedParameters);
    }

    public static <T> T findAndCallConstructor(Class<T> clazz, Object... parameters) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T> constructor = findConstructor(clazz, parameters);

        if (constructor == null) {
            return null;
        }

        HashMap<Class<?>, Object> parameterMap = new HashMap<>();

        for (Object parameter : parameters) {
            parameterMap.put(parameter.getClass(), parameter);
        }

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] orderedParameters = new Object[parameters.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            orderedParameters[i] = parameterMap.get(parameterTypes[i]);
        }

        return constructor.newInstance(orderedParameters);
    }

    public static <T> T findAndCallConstructor(Class<T> clazz, NamedParameter... parameters) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T> constructor = findConstructor(clazz, Arrays.stream(parameters).map(named -> named.getValue().getClass())
                .collect(Collectors.toSet()).toArray(new Class<?>[]{}));

        if (constructor == null) {
            return null;
        }

        HashMap<String, NamedParameter> parameterMap = new HashMap<>();

        for (NamedParameter parameter : parameters) {
            parameterMap.put(parameter.getName(), parameter);
        }

        Parameter[] constructorParameters = constructor.getParameters();
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] orderedParameters = new Object[parameters.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameter = constructorParameters[i];
            NamedParameter namedParameter = parameterMap.get(parameter.getName());

            if (namedParameter == null) {
                throw new NullPointerException("Unable to find named parameter for parameter :" + parameter.getName());
            }

            orderedParameters[i] = namedParameter.getValue();
        }

        return constructor.newInstance(orderedParameters);
    }

    public static <T> boolean isType(Object obj, Class<T> clazz) {
        return clazz.isInstance(obj) || (clazz.isPrimitive() && getWrapperType(clazz).isInstance(obj));
    }

    private static Class<?> getWrapperType(Class<?> clazz) {
        if (!clazz.isPrimitive()) return clazz;
        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == double.class) return Double.class;
        if (clazz == float.class) return Float.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == char.class) return Character.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == short.class) return Short.class;
        return Void.class;
    }

}
