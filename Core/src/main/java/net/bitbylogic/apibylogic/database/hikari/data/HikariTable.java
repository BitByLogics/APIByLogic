package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
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
                    object.ifPresent(o -> dataMap.put(statements.getId(o), o));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void add(@NotNull O object) {
        add(object, true);
    }

    public void add(@NotNull O object, boolean save) {
        if (dataMap.containsKey(statements.getId(object))) {
            return;
        }

        dataMap.put(statements.getId(object), object);

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
                    Field field = fieldObject.getClass().getDeclaredField(columnData.getFieldName());
                    boolean originalState = field.canAccess(this);
                    field.setAccessible(true);
                    field.setInt(object, result.getInt(1));
                    field.setAccessible(originalState);
                } catch (NoSuchFieldException | SQLException | IllegalAccessException e) {
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

    public void remove(@NotNull O object) {
        remove(object, false);
    }

    public void remove(@NotNull O object, boolean delete) {
        if (!dataMap.containsKey(statements.getId(object))) {
            return;
        }

        dataMap.remove(statements.getId(object));

        if (delete) {
            delete(object);
        }
    }

    public void delete(@NotNull O object) {
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

                if (columnData.getFieldClass().isEnum()) {
                    for (Object enumConstant : columnData.getFieldClass().getEnumConstants()) {
                        if (!((Enum<?>) enumConstant).name().equalsIgnoreCase((String) object)) {
                            continue;
                        }

                        object = enumConstant;
                        break;
                    }
                } else {
                    object = processor.parseFromObject(object);

                    if (columnData.getFieldClass() != String.class &&
                            processor instanceof DefaultHikariFieldProcessor &&
                            object instanceof String string) {
                        object = StringProcessor.findAndProcess(columnData.getFieldClass(), string);
                    }
                }

                if (statementData.foreignTable().isEmpty()) {
                    namedParameters.add(new NamedParameter(columnData.getFieldName(), columnData.getFieldClass(), object));
                    continue;
                }

                HikariTable<?> foreignTable = hikariAPI.getTable(statementData.foreignTable());

                if (foreignTable == null) {
                    System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to load object, missing foreign table for: "
                            + columnData.getStatementData().foreignTable());
                    continue;
                }

                if (columnData.getFieldClass().isInstance(HikariObject.class)) {
                    foreignTable.getDataFromDB(object, true, o -> {
                        namedParameters.add(new NamedParameter(columnData.getFieldName(), columnData.getFieldClass(), o.orElse(null)));
                    });
                    continue;
                }

                if (columnData.getFieldClass().isAssignableFrom(List.class)) {
                    List<Object> list = (List<Object>) ListUtil.stringToList((String) object);
                    List<HikariObject> newList = new ArrayList<>();

                    list.forEach(id -> {
                        foreignTable.getDataFromDB(id, true, o -> {
                            o.ifPresent(newList::add);
                        });
                    });

                    namedParameters.add(new NamedParameter(columnData.getFieldName(), columnData.getFieldClass(), newList));
                    continue;
                }

                Map<Object, Object> map = HashMapUtil.mapFromString(null, (String) object);
                HashMap<Object, HikariObject> newMap = new HashMap<>();
                map.forEach((key, value) -> {
                    foreignTable.getDataFromDB(value, true, o -> {
                        o.ifPresent(data -> newMap.put(key, data));
                    });
                });

                namedParameters.add(new NamedParameter(columnData.getFieldName(), columnData.getFieldClass(), newMap));
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

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, getStatements().getPrimaryKeyData().getFieldName(), id), result -> {
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

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, statements.getPrimaryKeyData().getFieldName(), id.toString()), result -> {
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
