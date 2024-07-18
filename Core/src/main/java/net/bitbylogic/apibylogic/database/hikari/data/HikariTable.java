package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
import net.bitbylogic.apibylogic.database.hikari.HikariAPI;
import net.bitbylogic.apibylogic.database.hikari.redis.HikariRedisUpdateType;
import net.bitbylogic.apibylogic.database.hikari.redis.HikariUpdateRedisMessageListener;
import net.bitbylogic.apibylogic.database.redis.client.RedisClient;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public abstract class HikariTable<O extends HikariObject> {

    private final HikariAPI hikariAPI;
    private final String table;
    private final ConcurrentHashMap<Object, O> dataMap;
    private String primaryKeyFieldName;

    private RedisClient redisClient;

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table, boolean loadData) {
        this.hikariAPI = hikariAPI;
        this.table = table;
        this.dataMap = new ConcurrentHashMap<>();

        try {
            O tempObject = objectClass.newInstance();
            hikariAPI.executeStatement(tempObject.getTableCreateStatement(table), resultSet -> {
                if (!loadData) {
                    return;
                }

                loadData();
            });
            primaryKeyFieldName = tempObject.getPrimaryKeyFieldName();
        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table.");
            e.printStackTrace();
        }
    }

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table) {
        this.hikariAPI = hikariAPI;
        this.table = table;
        this.dataMap = new ConcurrentHashMap<>();

        try {
            O tempObject = objectClass.newInstance();

            hikariAPI.executeStatement(tempObject.getTableCreateStatement(table), rs -> {
                loadData();
            });
            primaryKeyFieldName = tempObject.getPrimaryKeyFieldName();
        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table.");
            e.printStackTrace();
        }
    }

    private void loadData() {
        this.dataMap.clear();

        hikariAPI.executeQuery(String.format("SELECT * FROM %s;", table), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    Optional<O> object = loadObject(result);
                    object.ifPresent(o -> dataMap.put(o.getId(), o));
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
        if (dataMap.containsKey(object.getId())) {
            return;
        }

        dataMap.put(object.getId(), object);

        if (!save) {
            return;
        }

        save(object);
    }

    public void save(@NotNull O object) {
        save(object, null);
    }

    public void save(@NotNull O object, @Nullable Consumer<Optional<ResultSet>> callback) {
        hikariAPI.executeStatement(object.getDataSaveStatement(table), result -> {
            if (result == null) {
                if (callback != null) {
                    callback.accept(Optional.empty());
                }

                return;
            }

            object.getColumnData().forEach(columnData -> {
                if (!columnData.getStatementData().autoIncrement()) {
                    return;
                }

                Object fieldObject = object.getFieldObject(columnData);

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
                    .addData("updateType", HikariRedisUpdateType.SAVE).addData("objectId", object.getId().toString()));
        });
    }

    public void remove(@NotNull O object) {
        remove(object, false);
    }

    public void remove(@NotNull O object, boolean delete) {
        if (!dataMap.containsKey(object.getId())) {
            return;
        }

        dataMap.remove(object.getId());

        if (delete) {
            delete(object);
        }
    }

    public void delete(@NotNull O object) {
        hikariAPI.executeStatement(object.getDataDeleteStatement(table), rs -> {
            if (redisClient != null) {
                redisClient.sendListenerMessage(new ListenerComponent("", "hikari-update")
                        .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", object.getId().toString()));
            }
        });
    }

    public abstract Optional<O> loadObject(ResultSet set) throws SQLException;

    public Optional<O> getDataById(@NotNull Object id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public void getDataFromDB(@NotNull Object id, boolean checkCache, @NotNull Consumer<Optional<O>> callback) {
        if (checkCache) {
            for (O next : new ArrayList<>(dataMap.values())) {
                if (!next.getId().equals(id)) {
                    continue;
                }

                callback.accept(Optional.of(next));
                return;
            }
        }

        if (primaryKeyFieldName == null) {
            callback.accept(Optional.empty());
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, primaryKeyFieldName, id.toString()), result -> {
            if (result == null) {
                callback.accept(Optional.empty());
                return;
            }

            try {
                if (result.next()) {
                    callback.accept(loadObject(result));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            callback.accept(Optional.empty());
        });
    }

    public void getDataFromDB(@NotNull Object id, @NotNull Consumer<Optional<O>> consumer) {
        if (primaryKeyFieldName == null) {
            consumer.accept(Optional.empty());
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, primaryKeyFieldName, id.toString()), result -> {
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
