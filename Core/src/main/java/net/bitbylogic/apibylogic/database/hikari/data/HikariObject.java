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

    public abstract Object getId();

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

    public String getTableCreateStatement(String table) {
        StringBuilder builder = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (%s", table, getStatementDataBlock(true)));

        if (statementData.containsKey("#primary-key")) {
            builder.append(String.format(", PRIMARY KEY(%s)", statementData.get("#primary-key")));
        }

        return builder.append(");").toString();
    }

    public String getDataCreateStatement(String table) {
        return String.format("INSERT INTO %s (%s) VALUES(%S);", table, getStatementDataBlock(false), getValuesDataBlock());
    }

    public String getDataSaveStatement(String table, String... includedFields) {
        StringBuilder builder = new StringBuilder(String.format("INSERT INTO %s(%s) VALUES(%s) ON DUPLICATE KEY UPDATE",
                table, getStatementDataBlock(false, includedFields), getValuesDataBlock(includedFields)));

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

        return builder.append(String.join(", ", entries)).append(";").toString();
    }

    public String getUpdateStatement(String table, String... includedFields) {
        StringBuilder builder = new StringBuilder(String.format("UPDATE %s SET ", table));

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

    public String getDataDeleteStatement(String table) {
        if (!statementData.containsKey("#primary-key")) {
            System.out.printf("[APIByLogic] [HikariAPI] (%s) No primary key, aborting.%n", table);
            return null;
        }

        String primaryKey = statementData.get("#primary-key");
        StringBuilder builder = new StringBuilder(String.format("DELETE FROM %s WHERE %s=", table, primaryKey));

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
