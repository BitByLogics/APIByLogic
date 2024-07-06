package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.database.hikari.processor.HikariDataProcessor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Getter
public abstract class HikariObject {

    private final List<HikariColumnData> columnData = new ArrayList<>();
    private final HashMap<String, HikariDataProcessor<?>> processorMap = new HashMap<>();

    public abstract void loadProcessors();

    protected void loadStatementData() {
        loadStatementData(null, new ArrayList<>());
    }

    private void loadStatementData(@Nullable Object object, List<String> parentObjectFields) {
        List<Field> fields = new ArrayList<>();
        Object fieldObject = object == null ? this : object;

        fields.addAll(Arrays.asList(fieldObject.getClass().getFields()));
        fields.addAll(Arrays.asList(fieldObject.getClass().getDeclaredFields()));

        List<String> originalFields = new ArrayList<>(parentObjectFields);

        fields.forEach(field -> {
            if (!field.isAnnotationPresent(HikariStatementData.class)) {
                return;
            }

            HikariStatementData data = field.getAnnotation(HikariStatementData.class);

            if (data.subClass()) {
                try {
                    field.setAccessible(true);
                    parentObjectFields.add(field.getName());
                    loadStatementData(field.get(fieldObject), new ArrayList<>(parentObjectFields));
                    parentObjectFields.clear();
                    parentObjectFields.addAll(originalFields);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (data.dataType().isEmpty()) {
                System.out.println("(HikariObject): Skipped field " + field.getName() + ", you must provide a data type!");
                return;
            }

            columnData.add(new HikariColumnData(field.getName(), fieldObject.getClass().getName(), data, parentObjectFields));
        });
    }

    private String getStatementDataBlock(boolean includeMetadata, String... includedFields) {
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

    private String getValuesDataBlock(String... includedFields) {
        StringBuilder builder = new StringBuilder();

        List<String> data = new ArrayList<>();

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst().ifPresent(columnData -> {
            try {
                Object fieldObject = getFieldObject(columnData);

                HikariStatementData statementData = columnData.getStatementData();
                Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                field.setAccessible(true);
                Object fieldValue = field.get(fieldObject);

                if (statementData.processorID().isEmpty()) {
                    data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + (fieldValue instanceof Boolean ? ((Boolean) fieldValue) ? 1 : 0 : fieldValue.toString()) + "'"));
                    return;
                }

                HikariDataProcessor processor = processorMap.get(statementData.processorID());

                if (processor == null) {
                    System.out.println("(HikariObject): Invalid processor: " + statementData.processorID());
                    return;
                }

                data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + processor.processObject(fieldValue) + "'"));
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
                Object fieldObject = getFieldObject(columnData);

                HikariStatementData statementData = columnData.getStatementData();
                Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                field.setAccessible(true);
                Object fieldValue = field.get(fieldObject);

                if (statementData.processorID().isEmpty()) {
                    data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + (fieldValue instanceof Boolean ? ((Boolean) fieldValue) ? 1 : 0 : fieldValue.toString()) + "'"));
                    return;
                }

                HikariDataProcessor processor = processorMap.get(statementData.processorID());

                if (processor == null) {
                    System.out.println("(HikariObject): Invalid processor: " + statementData.processorID());
                    return;
                }

                data.add(String.format("%s", fieldValue == null ? "NULL" : "'" + processor.processObject(fieldValue) + "'"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        builder.append(String.join(", ", data));
        return builder.toString();
    }

    public String getTableCreateStatement(String table) {
        StringBuilder builder = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (%s", table, getStatementDataBlock(true)));

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst()
                .ifPresent(columnData -> builder.append(String.format(", PRIMARY KEY(%s)", columnData.getColumnName())));

        return builder.append(");").toString();
    }

    public String getDataCreateStatement(String table) {
        return String.format("INSERT INTO %s (%s) VALUES(%S);", table, getStatementDataBlock(false), getValuesDataBlock());
    }

    public String getDataSaveStatement(String table, String... includedFields) {
        StringBuilder builder = new StringBuilder(String.format("INSERT INTO %s (%s) VALUES(%s) ON DUPLICATE KEY UPDATE ",
                table, getStatementDataBlock(false, includedFields), getValuesDataBlock(includedFields)));

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

    public String getUpdateStatement(String table, String... includedFields) {
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
                Object fieldObject = getFieldObject(columnData);

                HikariStatementData statementData = columnData.getStatementData();
                Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                field.setAccessible(true);
                Object fieldValue = field.get(fieldObject);

                if (statementData.processorID().isEmpty()) {
                    entries.add(String.format("key= %s", fieldValue == null ? "NULL" : "'" + (fieldValue instanceof Boolean ? ((Boolean) fieldValue) ? 1 : 0 : fieldValue.toString()) + "'"));
                    return;
                }

                HikariDataProcessor processor = processorMap.get(statementData.processorID());

                if (processor == null) {
                    System.out.println("(HikariObject): Invalid processor: " + statementData.processorID());
                    return;
                }

                entries.add(String.format("key= %s", fieldValue == null ? null : "'" + processor.processObject(fieldValue) + "'"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst()
                .ifPresent(columnData -> {
                    try {
                        Object fieldObject = getFieldObject(columnData);

                        HikariStatementData statementData = columnData.getStatementData();
                        Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                        field.setAccessible(true);
                        Object fieldValue = field.get(fieldObject);

                        builder.append(String.join(", ", entries)).append(" WHERE ").append(columnData.getColumnName()).append(" = ");

                        if (statementData.processorID().isEmpty()) {
                            builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + (fieldValue instanceof Boolean ? ((Boolean) fieldValue) ? 1 : 0 : fieldValue.toString()) + "'"));
                            return;
                        }

                        HikariDataProcessor processor = processorMap.get(statementData.processorID());

                        if (processor == null) {
                            System.out.println("(HikariObject): Invalid processor: " + statementData.processorID());
                            return;
                        }

                        builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + processor.processObject(fieldValue) + "'"));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

        return builder.toString();
    }

    public String getDataDeleteStatement(String table) {
        if (columnData.stream().noneMatch(columnData -> columnData.getStatementData().primaryKey())) {
            System.out.printf("[APIByLogic] [HikariAPI] (%s) No primary key, aborting.%n", table);
            return null;
        }

        StringBuilder builder = new StringBuilder();

        columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey()).findFirst()
                .ifPresent(columnData -> {
                    builder.append(String.format("DELETE FROM %s WHERE %s=", table, columnData.getColumnName()));

                    try {
                        Object fieldObject = getFieldObject(columnData);

                        HikariStatementData statementData = columnData.getStatementData();
                        Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                        field.setAccessible(true);
                        Object fieldValue = field.get(fieldObject);

                        if (statementData.processorID().isEmpty()) {
                            builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + (fieldValue instanceof Boolean ? ((Boolean) fieldValue) ? 1 : 0 : fieldValue.toString()) + "'"));
                            return;
                        }

                        HikariDataProcessor processor = processorMap.get(statementData.processorID());

                        if (processor == null) {
                            System.out.println("(HikariObject): Invalid processor: " + statementData.processorID());
                            return;
                        }

                        builder.append(String.format("%s;", fieldValue == null ? "NULL" : "'" + processor.processObject(fieldValue) + "'"));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

        return builder.toString();
    }

    public Object getFieldObject(HikariColumnData columnData) {
        if (columnData.getParentObjectFields().isEmpty()) {
            return this;
        }

        Object object = this;

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

    public String getPrimaryKeyFieldName() {
        return columnData.stream().filter(columnData -> columnData.getStatementData().primaryKey())
                .findFirst().map(HikariColumnData::getFieldName).orElse(null);
    }

    public Object getId() {
        String primaryKeyFieldName = getPrimaryKeyFieldName();

        if (primaryKeyFieldName == null) {
            return null;
        }

        try {
            Field primaryKeyField = this.getClass().getDeclaredField(primaryKeyFieldName);
            primaryKeyField.setAccessible(true);
            return primaryKeyField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

}
