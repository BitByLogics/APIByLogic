package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.database.hikari.processor.HikariFieldProcessor;
import net.bitbylogic.apibylogic.util.HashMapUtil;
import net.bitbylogic.apibylogic.util.ListUtil;
import net.bitbylogic.apibylogic.util.reflection.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@RequiredArgsConstructor
@Getter
public class HikariStatements<O extends HikariObject> {

    private final String table;

    private final List<HikariColumnData> columnData = new ArrayList<>();
    private final HashMap<String, HikariFieldProcessor<?>> cachedProcessors = new HashMap<>();

    protected void loadColumnData(@NonNull Object object, List<String> parentObjectFields) {
        List<Field> fields = new ArrayList<>();

        fields.addAll(Arrays.asList(object.getClass().getFields()));
        fields.addAll(Arrays.asList(object.getClass().getDeclaredFields()));

        List<String> originalFields = new ArrayList<>(parentObjectFields);

        fields.forEach(field -> {
            if (!field.isAnnotationPresent(HikariStatementData.class)) {
                return;
            }

            HikariStatementData data = field.getAnnotation(HikariStatementData.class);

            if (!data.foreignTable().isEmpty() &&
                    (!field.getType().isInstance(HikariObject.class) &&
                            !ReflectionUtil.isListOf(field, HikariObject.class) &&
                            !ReflectionUtil.isMapOf(field, HikariObject.class))) {
                System.out.println("(HikariObject): Skipped field " + field.getName() + ", foreign classes must contain HikariObject!");
                return;
            }

            if (data.subClass()) {
                try {
                    field.setAccessible(true);
                    parentObjectFields.add(field.getName());
                    loadColumnData(field.get(object), new ArrayList<>(parentObjectFields));
                    parentObjectFields.clear();
                    parentObjectFields.addAll(originalFields);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (data.dataType().isEmpty() && data.foreignTable().isEmpty()) {
                System.out.println("(HikariObject): Skipped field " + field.getName() + ", you must provide a data type!");
                return;
            }

            columnData.add(new HikariColumnData(field.getName(), field.getType(), object.getClass().getName(), data, parentObjectFields, null, null));
        });
    }

    public String getTableCreateStatement() {
        StringBuilder builder = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (%s", table, getStatementDataBlock(true)));

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst()
                .ifPresent(columnData -> builder.append(String.format(", PRIMARY KEY(%s)", columnData.getColumnName())));

        return builder.append(");").toString();
    }

    protected String getStatementDataBlock(boolean includeMetadata, String... includedFields) {
        StringBuilder builder = new StringBuilder();

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();

        columnData.stream().filter(data -> data.getStatementData().primaryKey()).findFirst().ifPresent(columnData -> {
            keys.add(columnData.getColumnName());
            values.add(columnData.getFormattedData());
        });

        columnData.forEach(columnData -> {
            if (columnData.getStatementData().primaryKey() || columnData.getStatementData().autoIncrement()) {
                return;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(columnData.getFieldName()))) {
                return;
            }

            keys.add(columnData.getColumnName());
            values.add(columnData.getFormattedData());
        });

        if (includeMetadata) {
            builder.append(String.join(", ", values));
            return builder.toString();
        }

        builder.append(String.join(", ", keys));
        return builder.toString();
    }

    private String getValuesDataBlock(O object, String... includedFields) {
        StringBuilder builder = new StringBuilder();
        List<String> data = new ArrayList<>();

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst().ifPresent(columnData -> {
            try {
                Object fieldObject = getFieldObject(object, columnData);

                HikariStatementData statementData = columnData.getStatementData();
                Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                field.setAccessible(true);
                Object fieldValue = field.get(fieldObject);

                HikariFieldProcessor processor = cachedProcessors.get(columnData.getFieldName());

                if (processor == null) {
                    try {
                        processor = statementData.processor().getDeclaredConstructor().newInstance();
                        cachedProcessors.put(columnData.getFieldName(), processor);
                    } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (statementData.foreignTable().isEmpty()) {
                    data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + processor.parseToObject(fieldValue) + "'"));
                    return;
                }

                data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + getForeignFieldObject(object, field, columnData) + "'"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        columnData.forEach(columnData -> {
            if (columnData.getStatementData().primaryKey() || columnData.getStatementData().autoIncrement()) {
                return;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(columnData.getFieldName()))) {
                return;
            }

            try {
                Object fieldObject = getFieldObject(object, columnData);

                HikariStatementData statementData = columnData.getStatementData();
                Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                field.setAccessible(true);
                Object fieldValue = field.get(fieldObject);

                HikariFieldProcessor processor = cachedProcessors.get(columnData.getFieldName());

                if (processor == null) {
                    try {
                        processor = statementData.processor().getDeclaredConstructor().newInstance();
                        cachedProcessors.put(columnData.getFieldName(), processor);
                    } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (statementData.foreignTable().isEmpty()) {
                    data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + processor.parseToObject(fieldValue) + "'"));
                    return;
                }

                data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + getForeignFieldObject(object, field, columnData) + "'"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        builder.append(String.join(", ", data));
        return builder.toString();
    }

    protected String getDataDeleteStatement(O object, String table) {
        if (columnData.stream().noneMatch(columnData -> columnData.getStatementData().primaryKey())) {
            System.out.printf("[APIByLogic] [HikariAPI] (%s) No primary key, aborting.%n", table);
            return null;
        }

        StringBuilder builder = new StringBuilder();

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst()
                .ifPresent(columnData -> {
                    builder.append(String.format("DELETE FROM %s WHERE %s=", table, columnData.getColumnName()));

                    try {
                        Object fieldObject = getFieldObject(object, columnData);

                        HikariStatementData statementData = columnData.getStatementData();
                        Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                        field.setAccessible(true);
                        Object fieldValue = field.get(fieldObject);

                        HikariFieldProcessor processor = cachedProcessors.get(columnData.getFieldName());

                        if (processor == null) {
                            try {
                                processor = statementData.processor().getDeclaredConstructor().newInstance();
                                cachedProcessors.put(columnData.getFieldName(), processor);
                            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                                e.printStackTrace();
                                return;
                            }
                        }

                        if (statementData.foreignTable().isEmpty()) {
                            builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + processor.parseToObject(fieldValue) + "'"));
                            return;
                        }

                        builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + getForeignFieldObject(object, field, columnData) + "'"));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

        return builder.toString();
    }

    protected String getDataSaveStatement(O object, String... includedFields) {
        StringBuilder builder = new StringBuilder(String.format("INSERT INTO %s (%s) VALUES(%s) ON DUPLICATE KEY UPDATE ",
                table, getStatementDataBlock(false, includedFields), getValuesDataBlock(object, includedFields)));

        List<String> entries = new ArrayList<>();

        columnData.forEach(columnData -> {
            if (columnData.getStatementData().primaryKey() || columnData.getStatementData().autoIncrement()) {
                return;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(columnData.getFieldName()))) {
                return;
            }

            if (!columnData.getStatementData().updateOnSave()) {
                return;
            }

            entries.add(columnData.getColumnName() + "=VALUES(" + columnData.getColumnName() + ")");
        });

        return builder.append(String.join(", ", entries)).append(";").toString();
    }

    protected String getUpdateStatement(O object, String... includedFields) {
        StringBuilder builder = new StringBuilder(String.format("UPDATE %s SET ", table));

        List<String> entries = new ArrayList<>();

        columnData.forEach(columnData -> {
            if (columnData.getStatementData().primaryKey() || columnData.getStatementData().autoIncrement()) {
                return;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(columnData.getFieldName()))) {
                return;
            }

            try {
                Object fieldObject = getFieldObject(object, columnData);

                HikariStatementData statementData = columnData.getStatementData();
                Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                field.setAccessible(true);
                Object fieldValue = field.get(fieldObject);

                HikariFieldProcessor processor = cachedProcessors.get(columnData.getFieldName());

                if (processor == null) {
                    try {
                        processor = statementData.processor().getDeclaredConstructor().newInstance();
                        cachedProcessors.put(columnData.getFieldName(), processor);
                    } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (statementData.foreignTable().isEmpty()) {
                    entries.add(String.format("key= %s", fieldValue == null ? null : "'" + processor.parseToObject(fieldValue) + "'"));
                    return;
                }

                entries.add(String.format("key= %s", fieldValue == null ? null : "'" + getForeignFieldObject(object, field, columnData) + "'"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst()
                .ifPresent(columnData -> {
                    try {
                        Object fieldObject = getFieldObject(object, columnData);

                        HikariStatementData statementData = columnData.getStatementData();
                        Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                        field.setAccessible(true);
                        Object fieldValue = field.get(fieldObject);

                        builder.append(String.join(", ", entries)).append(" WHERE ").append(columnData.getColumnName()).append(" = ");

                        HikariFieldProcessor processor = cachedProcessors.get(columnData.getFieldName());

                        if (processor == null) {
                            try {
                                processor = statementData.processor().getDeclaredConstructor().newInstance();
                                cachedProcessors.put(columnData.getFieldName(), processor);
                            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                                e.printStackTrace();
                                return;
                            }
                        }

                        if (statementData.foreignTable().isEmpty()) {
                            builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + processor.parseToObject(fieldValue) + "'"));
                            return;
                        }

                        builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + getForeignFieldObject(object, field, columnData) + "'"));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

        return builder.toString();
    }

    protected Object getFieldObject(Object object, HikariColumnData columnData) {
        if (columnData.getParentObjectFields().isEmpty()) {
            return object;
        }

        for (int i = 0; i < columnData.getParentObjectFields().size(); i++) {
            String parentFieldName = columnData.getParentObjectFields().get(i);

            try {
                Field parentField = object.getClass().getDeclaredField(parentFieldName);
                parentField.setAccessible(true);
                object = parentField.get(object);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return object;
    }

    public Object getId(HikariObject object) {
        String primaryKeyFieldName = getPrimaryKeyData().getFieldName();

        if (primaryKeyFieldName == null) {
            return null;
        }

        try {
            Field primaryKeyField = object.getClass().getDeclaredField(primaryKeyFieldName);
            primaryKeyField.setAccessible(true);
            return primaryKeyField.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Object getForeignFieldObject(Object object, Field field, HikariColumnData columnData) {
        if (columnData.getStatementData().foreignTable().isEmpty()) {
            return null;
        }

        field.setAccessible(true);

        try {
            HikariTable<?> foreignTable = columnData.getForeignTable();
            Object fieldValue = field.get(object);

            if (field.getType().isInstance(HikariObject.class)) {
                return foreignTable.getStatements().getId((HikariObject) fieldValue);
            }

            Type fieldType = field.getGenericType();

            if (fieldType instanceof ParameterizedType parameterizedType) {
                Class<?> fieldClass = (Class<?>) parameterizedType.getRawType();

                if (List.class.isAssignableFrom(fieldClass)) {
                    List<HikariObject> list = (List<HikariObject>) fieldValue;
                    List<Object> newList = new ArrayList<>();
                    list.forEach(hikariObject -> newList.add(foreignTable.getStatements().getId(hikariObject)));

                    return ListUtil.listToString(newList);
                }

                if (!Map.class.isAssignableFrom(fieldClass)) {
                    return fieldValue;
                }

                Map<Object, HikariObject> map = (Map<Object, HikariObject>) fieldValue;
                HashMap<Object, Object> newMap = new HashMap<>();
                map.forEach((key, value) -> newMap.put(key, foreignTable.getStatements().getId(value)));

                return HashMapUtil.mapToString(newMap);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public HikariColumnData getPrimaryKeyData() {
        return columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst().orElse(null);
    }

}
