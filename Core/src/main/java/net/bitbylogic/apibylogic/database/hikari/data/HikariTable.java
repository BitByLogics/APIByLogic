package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.database.hikari.HikariAPI;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.database.hikari.processor.HikariFieldProcessor;
import net.bitbylogic.apibylogic.database.hikari.processor.impl.DefaultHikariFieldProcessor;
import net.bitbylogic.apibylogic.database.hikari.redis.HikariRedisUpdateType;
import net.bitbylogic.apibylogic.database.hikari.redis.HikariUpdateRedisMessageListener;
import net.bitbylogic.apibylogic.database.redis.client.RedisClient;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.util.HashMapUtil;
import net.bitbylogic.apibylogic.util.ListUtil;
import net.bitbylogic.apibylogic.util.Pair;
import net.bitbylogic.apibylogic.util.StringProcessor;
import net.bitbylogic.apibylogic.util.reflection.NamedParameter;
import net.bitbylogic.apibylogic.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public class HikariTable<O extends HikariObject> {

    private final HikariAPI hikariAPI;

    private final String table;
    private final boolean loadData;
    private final Class<O> objectClass;

    private final ConcurrentHashMap<Object, O> dataMap;
    private final HikariStatements<O> statements;

    private Constructor<O> objectConstructor;
    private RedisClient redisClient;

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table, boolean loadData) {
        this.hikariAPI = hikariAPI;
        this.table = table;
        this.loadData = loadData;
        this.objectClass = objectClass;
        this.dataMap = new ConcurrentHashMap<>();
        this.statements = new HikariStatements<>(table);

        try {
            Constructor<O> emptyConstructor = ReflectionUtil.findConstructor(objectClass);

            if (emptyConstructor == null) {
                log("Unable to create table, missing main constructor.");
                return;
            }

            O tempObject = emptyConstructor.newInstance();
            statements.loadColumnData(tempObject, new ArrayList<>());

            List<NamedParameter> namedParameters = statements.getColumnData().stream().map(data -> data.asNamedParameter(null)).toList();
            objectConstructor = ReflectionUtil.findNamedConstructor(objectClass, namedParameters.toArray(new NamedParameter[]{}));

            if (objectConstructor == null) {
                log("Unable to create table, missing main constructor.");
                return;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log("Unable to create table.");
            e.printStackTrace();
        }
    }

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table) {
        this(hikariAPI, objectClass, table, true);
    }

    public void loadData(@NonNull Runnable completeRunnable) {
        dataMap.clear();

        log("Retrieving data from database...");
        hikariAPI.executeQuery(String.format("SELECT * FROM %s;", table), result -> {
            try {
                if (result == null || !result.next()) {
                    log("Finished retrieving data.");

                    completeRunnable.run();
                    onDataLoaded();
                    return;
                }

                do {
                    loadObject(result, o -> o.ifPresent(data -> {
                        dataMap.put(statements.getId(data), data);
                        data.setOwningTable(this);
                    }));
                } while (result.next());

                log("Finished retrieving data.");

                completeRunnable.run();
                onDataLoaded();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public void onDataLoaded() {
    }

    public void onDataDeleted(@NonNull O object) {
    }

    public void onDataAdded(@NonNull O object) {
    }

    public void add(@NotNull O object) {
        add(object, true);
    }

    public void add(@NotNull O object, boolean save) {
        if (dataMap.containsKey(statements.getId(object))) {
            return;
        }

        dataMap.put(statements.getId(object), object);
        object.setOwningTable(this);
        onDataAdded(object);

        if (!save) {
            return;
        }

        save(object);
    }

    public void save(@NotNull O object) {
        save(object, null);
    }

    public void save(@NotNull O object, @Nullable Consumer<Optional<ResultSet>> callback) {
        hikariAPI.executeStatement(statements.getDataSaveStatement(object), result -> {
            if (result == null) {
                if (callback != null) {
                    callback.accept(Optional.empty());
                }

                return;
            }

            statements.getColumnData().forEach(columnData -> {
                if (!columnData.getStatementData().autoIncrement()) {
                    return;
                }

                Object fieldObject = statements.getFieldObject(object, columnData);

                try {
                    Field field = columnData.getField();
                    boolean originalState = field.canAccess(this);
                    field.setAccessible(true);
                    field.setInt(object, result.getInt(1));
                    field.setAccessible(originalState);
                } catch (SQLException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });

            if (callback != null) {
                callback.accept(Optional.of(result));
            }

            if (redisClient == null) {
                return;
            }

            redisClient.sendListenerMessage(new ListenerComponent("", "hikari-update")
                    .addData("updateType", HikariRedisUpdateType.SAVE).addData("objectId", statements.getId(object).toString()));
        });
    }

    public void deleteById(@NotNull Object id) {
        getDataFromDB(id, true, (data) -> {
            if (data.isEmpty()) {
                return;
            }

            delete(data.get());
        });
    }

    public void delete(@NotNull O object) {
        if (!dataMap.containsKey(statements.getId(object))) {
            return;
        }

        dataMap.remove(statements.getId(object));

        for (HikariColumnData columnData : statements.getColumnData()) {
            if (columnData.getForeignTable() == null || !columnData.getStatementData().foreignDelete()) {
                continue;
            }

            try {
                Object fieldObject = statements.getFieldObject(object, columnData);

                Field field = columnData.getField();
                field.setAccessible(true);

                Object foreignObject = statements.getForeignFieldObject(object, field, columnData);

                if (foreignObject instanceof List<?> list) {
                    if (list.isEmpty()) {
                        continue;
                    }

                    List<HikariObject> dataList = (List<HikariObject>) foreignObject;
                    dataList.forEach(data -> columnData.getForeignTable().deleteById(statements.getId(data)));
                    continue;
                }

                if (foreignObject instanceof HashMap<?, ?> hashMap) {
                    if (hashMap.isEmpty()) {
                        continue;
                    }

                    HashMap<?, HikariObject> dataMap = (HashMap<?, HikariObject>) foreignObject;
                    dataMap.values().forEach(data -> columnData.getForeignTable().deleteById(statements.getId(data)));
                    continue;
                }

                if (!(foreignObject instanceof HikariObject)) {
                    continue;
                }

                columnData.getForeignTable().deleteById(statements.getId((HikariObject) foreignObject));
            } catch (Exception e) {
                log("Unable to delete foreign data.");
                e.printStackTrace();
            }

            onDataDeleted(object);
        }

        hikariAPI.executeStatement(statements.getDataDeleteStatement(object, table), rs -> {
            if (redisClient != null) {
                redisClient.sendListenerMessage(new ListenerComponent("", "hikari-update")
                        .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", statements.getId(object).toString()));
            }
        });
    }

    protected void loadPendingData(@NonNull Pair<Field, Object> key, @NonNull Object object, Consumer<Object> consumer) {
        Field field = key.getKey();
        Object value = key.getValue();

        Class<?> primaryKeyType = statements.getPrimaryKeyData().getField().getType();
        Class<?> fieldType = field.getType();

        if (value == null || ((value instanceof String) && ((String) value).trim().isEmpty())) {
            consumer.accept(
                    fieldType.isAssignableFrom(List.class) ? new ArrayList<>() :
                            fieldType.isAssignableFrom(Map.class) ? new HashMap<>() : null);
            return;
        }

        if (fieldType.isInstance(HikariObject.class)) {
            if (value instanceof String) {
                value = StringProcessor.findAndProcess(primaryKeyType, (String) value);
            }

            getDataFromDB(value, true, true, o -> o.ifPresent(consumer));
            return;
        }

        if (fieldType.isAssignableFrom(List.class)) {
            if (!(value instanceof String)) {
                log("Unable to process field: " + field.getName() + " for class " + object.getClass().getSimpleName() + ".");
                consumer.accept(new ArrayList<>());
                return;
            }

            List<Object> list = (List<Object>) ListUtil.stringToList((String) value);
            List<O> newList = new ArrayList<>();

            if (list.isEmpty()) {
                consumer.accept(new ArrayList<>());
                return;
            }

            AtomicInteger loadedData = new AtomicInteger();

            list.forEach(id -> {
                if (!(id instanceof String)) {
                    log("Unable to process item in list: " + id.toString() + " for class " + object.getClass().getSimpleName() + ".");
                    loadedData.incrementAndGet();
                    return;
                }

                id = StringProcessor.findAndProcess(primaryKeyType, (String) id);
                getDataFromDB(id, true, true, o -> {
                    synchronized (newList) {
                        o.ifPresent(newList::add);
                    }

                    if (loadedData.incrementAndGet() != list.size()) {
                        return;
                    }

                    consumer.accept(newList);
                });
            });
            return;
        }

        if (fieldType.isAssignableFrom(Map.class) || !(value instanceof String)) {
            log("Unable to process field: " + field.getName() + " for class " + object.getClass().getSimpleName() + ".");
            consumer.accept(new HashMap<>());
            return;
        }

        Map<Object, Object> map = HashMapUtil.mapFromString(null, (String) value);
        HashMap<Object, O> newMap = new HashMap<>();

        if (map.isEmpty()) {
            consumer.accept(newMap);
            return;
        }

        AtomicInteger loadedData = new AtomicInteger();

        map.forEach((key1, id) -> {
            if (!(id instanceof String)) {
                log("Unable to process item in map: " + id.toString() + " for class " + object.getClass().getSimpleName() + ".");
                loadedData.incrementAndGet();
                return;
            }

            id = StringProcessor.findAndProcess(primaryKeyType, (String) id);

            getDataFromDB(id, true, true, o -> {
                synchronized (newMap) {
                    o.ifPresent(data -> newMap.put(key1, data));
                }

                if (loadedData.incrementAndGet() != newMap.size()) {
                    return;
                }

                consumer.accept(newMap);
            });
        });
    }

    public void loadObject(ResultSet result, Consumer<Optional<O>> consumer) throws SQLException {
        try {
            List<NamedParameter> namedParameters = new ArrayList<>();

            for (HikariColumnData columnData : statements.getColumnData()) {
                HikariStatementData statementData = columnData.getStatementData();
                HikariFieldProcessor processor = ReflectionUtil.findAndCallConstructor(columnData.getStatementData().processor());

                if (processor == null) {
                    log("Unable to load object, invalid processor '" + columnData.getStatementData().processor() + ".");
                    consumer.accept(Optional.empty());
                    return;
                }

                Object object = result.getObject(columnData.getColumnName());
                Class<?> fieldTypeClass = columnData.getField().getType();

                if (fieldTypeClass.isEnum()) {
                    for (Object enumConstant : fieldTypeClass.getEnumConstants()) {
                        if (!((Enum<?>) enumConstant).name().equalsIgnoreCase((String) object)) {
                            continue;
                        }

                        object = enumConstant;
                        break;
                    }
                } else {
                    object = processor.parseFromObject(object);

                    if (fieldTypeClass != String.class &&
                            processor instanceof DefaultHikariFieldProcessor &&
                            object instanceof String string) {
                        object = StringProcessor.findAndProcess(fieldTypeClass, string);
                    }
                }

                if (statementData.foreignTable().isEmpty()) {
                    namedParameters.add(new NamedParameter(columnData.getField().getName(), fieldTypeClass, object));

                    if (namedParameters.size() < statements.getColumnData().size()) {
                        continue;
                    }

                    O data = ReflectionUtil.callConstructor(objectConstructor, namedParameters.toArray(new NamedParameter[]{}));
                    consumer.accept(Optional.of(data));
                    continue;
                }

                HikariTable<?> foreignTable = hikariAPI.getTable(statementData.foreignTable());

                if (foreignTable == null) {
                    log("Unable to load object, missing foreign table: " + columnData.getStatementData().foreignDelete());
                    continue;
                }

                foreignTable.loadPendingData(new Pair<>(columnData.getField(), object), foreignTable, value -> {
                    namedParameters.add(new NamedParameter(columnData.getField().getName(), fieldTypeClass, value));

                    if (namedParameters.size() < statements.getColumnData().size()) {
                        return;
                    }

                    try {
                        O data = ReflectionUtil.callConstructor(objectConstructor, namedParameters.toArray(new NamedParameter[]{}));

                        consumer.accept(Optional.of(data));
                    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        consumer.accept(Optional.empty());
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log("Unable to load object.");
            throw new RuntimeException(e);
        }

        consumer.accept(Optional.empty());
    }

    public Optional<O> getDataById(@NotNull Object id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public void getDataFromDB(@NotNull Object id, boolean checkCache, @NotNull Consumer<Optional<O>> consumer) {
        getDataFromDB(id, checkCache, true, consumer);
    }

    public void getDataFromDB(@NotNull Object id, boolean checkCache, boolean cache, @NotNull Consumer<Optional<O>> consumer) {
        if (checkCache) {
            if (dataMap.containsKey(id)) {
                consumer.accept(Optional.of(dataMap.get(id)));
                return;
            }
        }

        if (statements.getPrimaryKeyData() == null) {
            consumer.accept(Optional.empty());
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, statements.getPrimaryKeyData().getColumnName(), id), result -> {
            try {
                if (result == null || !result.next()) {
                    consumer.accept(Optional.empty());
                    return;
                }

                loadObject(result, o -> {
                    try {
                        boolean last = result.isLast();

                        if (o.isEmpty()) {
                            if (!last) {
                                return;
                            }

                            consumer.accept(Optional.empty());
                            return;
                        }

                        O data = o.get();

                        if (cache && !dataMap.containsKey(statements.getId(data))) {
                            dataMap.put(statements.getId(data), data);
                            data.setOwningTable(this);
                        }

                        consumer.accept(Optional.of(data));
                    } catch (SQLException e) {
                        consumer.accept(Optional.empty());
                        throw new RuntimeException(e);
                    }
                });
            } catch (SQLException exception) {
                consumer.accept(Optional.empty());
                throw new RuntimeException(exception);
            }
        });
    }

    public void registerRedisHook(RedisClient redisClient) {
        this.redisClient = redisClient;
        redisClient.registerListener(new HikariUpdateRedisMessageListener<O>(this));
    }

    private void log(@NonNull String message) {
        System.out.println(String.format("[HikariAPI] [%s]: %s", getClass().getSimpleName(), message));
    }

}
