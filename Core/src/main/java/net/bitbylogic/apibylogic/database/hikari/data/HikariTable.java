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
                System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table, missing empty constructor.");
                return;
            }

            O tempObject = emptyConstructor.newInstance();
            statements.loadColumnData(tempObject, new ArrayList<>());

            List<NamedParameter> namedParameters = statements.getColumnData().stream().map(data -> data.asNamedParameter(null)).toList();
            objectConstructor = ReflectionUtil.findNamedConstructor(objectClass, namedParameters.toArray(new NamedParameter[]{}));

            if (objectConstructor == null) {
                System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table, missing main constructor.");
                return;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table.");
            e.printStackTrace();
        }
    }

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table) {
        this(hikariAPI, objectClass, table, true);
    }

    public void loadData() {
        this.dataMap.clear();

        hikariAPI.executeQuery(String.format("SELECT * FROM %s;", table), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    Optional<O> object = loadObject(result);
                    object.ifPresent(o -> {
                        dataMap.put(statements.getId(o), o);
                        o.setOwningTable(this);
                    });
                }

                onDataLoaded();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void onDataLoaded() {}

    public void onDataDeleted(@NonNull O object) {}

    public void onDataAdded(@NonNull O object) {}

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
                    e.printStackTrace();
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
                System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to delete foreign data.");
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

    public Optional<O> loadObject(ResultSet result) throws SQLException {
        try {
            List<NamedParameter> namedParameters = new ArrayList<>();

            for (HikariColumnData columnData : statements.getColumnData()) {
                HikariStatementData statementData = columnData.getStatementData();
                HikariFieldProcessor processor = ReflectionUtil.findAndCallConstructor(columnData.getStatementData().processor());

                if (processor == null) {
                    System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to load object, invalid processor.");
                    return Optional.empty();
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
                    continue;
                }

                HikariTable<?> foreignTable = hikariAPI.getTable(statementData.foreignTable());

                if (foreignTable == null) {
                    System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to load object, missing foreign table for: "
                            + columnData.getStatementData().foreignTable());
                    continue;
                }

                if (fieldTypeClass.isInstance(HikariObject.class)) {
                    foreignTable.getDataFromDB(object, true, o -> {
                        namedParameters.add(new NamedParameter(columnData.getField().getName(), fieldTypeClass, o.orElse(null)));
                    });
                    continue;
                }

                if (fieldTypeClass.isAssignableFrom(List.class)) {
                    List<Object> list = (List<Object>) ListUtil.stringToList((String) object);
                    List<HikariObject> newList = new ArrayList<>();

                    list.forEach(id -> {
                        foreignTable.getDataFromDB(id, true, o -> {
                            o.ifPresent(newList::add);
                        });
                    });

                    namedParameters.add(new NamedParameter(columnData.getField().getName(), fieldTypeClass, newList));
                    continue;
                }

                Map<Object, Object> map = HashMapUtil.mapFromString(null, (String) object);
                HashMap<Object, HikariObject> newMap = new HashMap<>();
                map.forEach((key, value) -> {
                    foreignTable.getDataFromDB(value, true, o -> {
                        o.ifPresent(data -> newMap.put(key, data));
                    });
                });

                namedParameters.add(new NamedParameter(columnData.getField().getName(), fieldTypeClass, newMap));
            }

            return Optional.of(ReflectionUtil.callConstructor(objectConstructor, namedParameters.toArray(new NamedParameter[]{})));
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to load object.");
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<O> getDataById(@NotNull Object id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public void getDataFromDB(@NotNull Object id, boolean checkCache, @NotNull Consumer<Optional<O>> callback) {
        if (checkCache) {
            for (O next : new ArrayList<>(dataMap.values())) {
                if (!statements.getId(next).equals(id)) {
                    continue;
                }

                callback.accept(Optional.of(next));
                return;
            }
        }

        if (statements.getPrimaryKeyData() == null) {
            callback.accept(Optional.empty());
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, getStatements().getPrimaryKeyData().getColumnName(), id), result -> {
            if (result == null) {
                callback.accept(Optional.empty());
                return;
            }

            try {
                if (result.next()) {
                    callback.accept(loadObject(result));
                    return;
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            callback.accept(Optional.empty());
        });
    }

    public void getDataFromDB(@NotNull Object id, @NotNull Consumer<Optional<O>> consumer) {
        if (statements.getPrimaryKeyData() == null) {
            consumer.accept(Optional.empty());
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, statements.getPrimaryKeyData().getColumnName(), id.toString()), result -> {
            if (result == null) {
                consumer.accept(Optional.empty());
                return;
            }

            try {
                if (result.next()) {
                    consumer.accept(loadObject(result));
                    return;
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });

        consumer.accept(Optional.empty());
    }

    public void registerRedisHook(RedisClient redisClient) {
        this.redisClient = redisClient;
        redisClient.registerListener(new HikariUpdateRedisMessageListener<O>(this));
    }

}
