package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.database.hikari.processor.HikariDataProcessor;

import java.lang.reflect.Field;
import java.util.*;

@Getter
public abstract class HikariObject {

    private final HashMap<String, String> statementData = new HashMap<>();
    private final HashMap<String, HikariStatementData> rawStatementData = new HashMap<>();
    private final HashMap<String, String> fieldNames = new HashMap<>();
    private final HashMap<String, HikariDataProcessor> processorMap = new HashMap<>();

    public HikariObject() {
        loadProcessors();
        loadStatementData();
    }

    public abstract Object getDataId();

    public abstract String getTableId();

    public abstract Object[] getDataObjects();

    public abstract void loadProcessors();

    private void loadStatementData() {
        List<Field> fields = new ArrayList<>();

        fields.addAll(Arrays.asList(getClass().getFields()));
        fields.addAll(Arrays.asList(getClass().getDeclaredFields()));

        fields.forEach(field -> {
            if (field.isAnnotationPresent(HikariStatementData.class)) {
                HikariStatementData data = field.getAnnotation(HikariStatementData.class);

                if (data.primaryKey()) {
                    statementData.put("#primary-key", field.getName().toLowerCase());
                }

                if (data.autoIncrement()) {
                    if (statementData.containsKey("#auto-increment")) {
                        System.out.println("(HikariObject): Error generating statement data, duplicate auto increment keys");
                        return;
                    }

                    statementData.put("#auto-increment", field.getName().toLowerCase());
                }

                statementData.put(field.getName().toLowerCase(), getFormattedData(field.getName().toLowerCase(), data));
                rawStatementData.put(field.getName().toLowerCase(), data);
                fieldNames.put(field.getName().toLowerCase(), field.getName());
            }
        });
    }

    private String getStatementDataBlock(boolean includeMetadata, String... includedFields) {
        StringBuilder builder = new StringBuilder();

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();

        if (statementData.containsKey("#primary-key")) {
            keys.add(statementData.get("#primary-key"));
            values.add(statementData.get(statementData.get("#primary-key")));
        }

        for (Map.Entry<String, String> entry : statementData.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("#primary-key")
                    || entry.getKey().equalsIgnoreCase("#auto-increment")
                    || statementData.getOrDefault("#primary-key", "").equalsIgnoreCase(entry.getKey())) {
                continue;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(entry.getKey()))) {
                continue;
            }

            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

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

        if (statementData.containsKey("#primary-key")) {
            String primaryKey = statementData.get("#primary-key");

            try {
                HikariStatementData rawStatementData = getRawStatementData().get(primaryKey);
                Field field = this.getClass().getDeclaredField(fieldNames.get(primaryKey));
                field.setAccessible(true);
                Object fieldObject = field.get(this);

                if (!rawStatementData.processorID().isEmpty()) {
                    HikariDataProcessor processor = processorMap.get(rawStatementData.processorID());
                    data.add(String.format("'%s'", processor.processObject(fieldObject)));
                } else {
                    data.add(String.format("'%s'", fieldObject.toString()));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        for (Map.Entry<String, String> entry : statementData.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("#primary-key")
                    || entry.getKey().equalsIgnoreCase("#auto-increment")
                    || statementData.getOrDefault("#primary-key", "").equalsIgnoreCase(entry.getKey())) {
                continue;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(entry.getKey()))) {
                continue;
            }

            try {
                HikariStatementData rawStatementData = getRawStatementData().get(entry.getKey());
                Field field = this.getClass().getDeclaredField(fieldNames.get(entry.getKey()));
                field.setAccessible(true);
                Object fieldObject = field.get(this);

                if (!rawStatementData.processorID().isEmpty()) {
                    HikariDataProcessor processor = processorMap.get(rawStatementData.processorID());
                    data.add(String.format("'%s'", processor.processObject(fieldObject)));
                    continue;
                }

                data.add(String.format("'%s'", fieldObject == null ? null : fieldObject.toString()));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        builder.append(String.join(", ", data));
        return builder.toString();
    }

    public String getTableCreateStatement() {
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getTableId()).append("(").append(getStatementDataBlock(true));

        if (statementData.containsKey("#primary-key")) {
            builder.append(", PRIMARY KEY(").append(statementData.get("#primary-key")).append(")");
        }

        builder.append(");");

        return builder.toString();
    }

    public String getDataCreateStatement() {
        return "INSERT INTO " + getTableId() + "(" + getStatementDataBlock(false) + ") VALUES(" + getValuesDataBlock() + ");";
    }

    public String getDataSaveStatement(String... includedFields) {
        StringBuilder builder = new StringBuilder("INSERT INTO ").append(getTableId())
                .append("(").append(getStatementDataBlock(false, includedFields))
                .append(") VALUES(").append(getValuesDataBlock(includedFields))
                .append(") ON DUPLICATE KEY UPDATE ");

        List<String> entries = new ArrayList<>();

        for (Map.Entry<String, String> entry : statementData.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("#primary-key") || entry.getKey().equalsIgnoreCase("#auto-increment")) {
                continue;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(entry.getKey()))) {
                continue;
            }

            String key = entry.getKey();

            if (!rawStatementData.get(key).updateOnSave()) {
                continue;
            }

            entries.add(key + "=VALUES(" + key + ")");
        }

        builder.append(String.join(", ", entries)).append(";");
        return builder.toString();
    }

    public String getUpdateStatement(String... includedFields) {
        StringBuilder builder = new StringBuilder("UPDATE ").append(getTableId())
                .append(" SET ");

        List<String> entries = new ArrayList<>();

        for (Map.Entry<String, String> entry : statementData.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("#primary-key") || entry.getKey().equalsIgnoreCase("#auto-increment")) {
                continue;
            }

            if (includedFields.length != 0 && Arrays.stream(includedFields).noneMatch(field -> field.equalsIgnoreCase(entry.getKey()))) {
                continue;
            }

            String key = entry.getKey();

            try {
                HikariStatementData rawStatementData = getRawStatementData().get(key);
                Field field = this.getClass().getDeclaredField(fieldNames.get(key));
                field.setAccessible(true);
                Object fieldObject = field.get(this);

                if (!rawStatementData.processorID().isEmpty()) {
                    HikariDataProcessor processor = processorMap.get(rawStatementData.processorID());
                    entries.add(String.format("key= '%s'", processor.processObject(fieldObject)));
                    continue;
                }

                entries.add(String.format("key= '%s'", fieldObject.toString()));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        String primaryKey = statementData.get("#primary-key");

        try {
            HikariStatementData rawStatementData = getRawStatementData().get(primaryKey);
            Field field = this.getClass().getDeclaredField(fieldNames.get(primaryKey));
            field.setAccessible(true);
            Object fieldObject = field.get(this);

            builder.append(String.join(", ", entries)).append(" WHERE ").append(primaryKey).append(" = ");

            if (!rawStatementData.processorID().isEmpty()) {
                HikariDataProcessor processor = processorMap.get(rawStatementData.processorID());
                builder.append(String.format("'%s';", processor.processObject(fieldObject)));
                return builder.toString();
            }

            builder.append(String.format("'%s';", fieldObject.toString()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }

    public String getDataDeleteStatement() {
        if (!statementData.containsKey("#primary-key")) {
            System.out.println(String.format("[APIByLogic] [HikariAPI] (%s) No primary key, aborting.", getTableId()));
            return null;
        }

        String primaryKey = statementData.get("#primary-key");
        StringBuilder builder = new StringBuilder("DELETE FROM ").append(getTableId())
                .append(" WHERE ").append(primaryKey).append("=");

        try {
            HikariStatementData rawStatementData = getRawStatementData().get(primaryKey);
            Field field = this.getClass().getDeclaredField(fieldNames.get(primaryKey));
            field.setAccessible(true);
            Object fieldObject = field.get(this);

            if (!rawStatementData.processorID().isEmpty()) {
                HikariDataProcessor processor = processorMap.get(rawStatementData.processorID());
                builder.append(String.format("'%s';", processor.processObject(fieldObject)));
                return builder.toString();
            }

            builder.append(String.format("'%s';", fieldObject.toString()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }

    private String getFormattedData(String fieldName, HikariStatementData data) {
        return fieldName + " " + data.dataType() + " " + (data.allowNull() ? "" : "NOT NULL") + (statementData.getOrDefault("#auto-increment", "").equalsIgnoreCase(fieldName) ? " AUTO_INCREMENT" : "");
    }

}
