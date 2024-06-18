package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.Getter;
import net.bitbylogic.apibylogic.database.hikari.HikariAPI;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.database.hikari.redis.HikariRedisUpdateType;
import net.bitbylogic.apibylogic.database.hikari.redis.HikariUpdateRedisMessageListener;
import net.bitbylogic.apibylogic.database.redis.client.RedisClient;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Getter
public abstract class HikariTable<O extends HikariObject> {

    private final HikariAPI hikariAPI;
    private final String table;
    private final ConcurrentHashMap<Object, O> dataMap;
    private String idFieldName;

    private RedisClient redisClient;

    public HikariTable(HikariAPI hikariAPI, Class<O> objectClass, String table, boolean loadData) {
        this.hikariAPI = hikariAPI;
        this.table = table;
        this.dataMap = new ConcurrentHashMap<>();

        try {
            O tempObject = objectClass.newInstance();
            hikariAPI.executeStatement(tempObject.getTableCreateStatement(table));
            Map.Entry<String, HikariStatementData> data = tempObject.getRawStatementData().entrySet().stream().filter(entry -> entry.getValue().primaryKey()).findFirst().orElse(null);
            idFieldName = data == null ? null : data.getKey();
        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table.");
            e.printStackTrace();
        }

        if (!loadData) {
            return;
        }

        loadData();
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
            Map.Entry<String, HikariStatementData> data = tempObject.getRawStatementData().entrySet().stream().filter(entry -> entry.getValue().primaryKey()).findFirst().orElse(null);
            idFieldName = data == null ? null : data.getKey();
        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("[APIByLogic] [HikariAPI] (" + table + "): Unable to create table.");
            e.printStackTrace();
        }

        loadData();
    }

    private void loadData() {
        this.dataMap.clear();

        hikariAPI.executeQuery(String.format("SELECT * FROM %s;", table), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    O object = loadObject(result);
                    this.dataMap.put(object.getId(), object);
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void add(O object) {
        add(object, true);
    }

    public void add(O object, boolean save) {
        if (dataMap.containsKey(object.getId())) {
            return;
        }

        dataMap.put(object.getId(), object);

        if (save) {
            CompletableFuture.runAsync(() -> save(object));
        }
    }

    public void save(O object) {
        save(object, null);
    }

    public void save(O object, Consumer<ResultSet> result) {
        if (object.getRawStatementData().isEmpty()) {
            return;
        }

        hikariAPI.executeStatement(object.getDataSaveStatement(table), rs -> {
            object.getRawStatementData().forEach((s, data) -> {
                if (!data.autoIncrement()) {
                    return;
                }

                try {
                    Field field = object.getClass().getDeclaredField(s);
                    boolean originalState = field.canAccess(this);
                    field.setAccessible(true);
                    field.setInt(object, rs.getInt(1));
                    field.setAccessible(originalState);
                } catch (NoSuchFieldException | SQLException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            });

            if (result != null) {
                result.accept(rs);
            }

            if (redisClient != null) {
                redisClient.sendListenerMessage(new ListenerComponent("", "hikari-update")
                        .addData("updateType", HikariRedisUpdateType.SAVE).addData("objectId", object.getId().toString()));
            }
        });
    }

    public void remove(O object) {
        remove(object, true);
    }

    public void remove(O object, boolean delete) {
        if (!dataMap.containsKey(object.getId())) {
            return;
        }

        dataMap.remove(object.getId());

        if (delete) {
            CompletableFuture.runAsync(() -> delete(object));
        }
    }

    public void delete(O object) {
        if (object.getRawStatementData().isEmpty()) {
            return;
        }

        hikariAPI.executeStatement(object.getDataDeleteStatement(table), rs -> {
            if (redisClient != null) {
                redisClient.sendListenerMessage(new ListenerComponent("", "hikari-update")
                        .addData("updateType", HikariRedisUpdateType.DELETE).addData("objectId", object.getId().toString()));
            }
        });
    }

    public abstract O loadObject(ResultSet set) throws SQLException;

    public O getDataById(Object id) {
        return dataMap.get(id);
    }

    public O getDataFromDB(Object id, boolean checkCache) {
        if (checkCache) {
            for (Iterator<O> iterator = new ArrayList<>(dataMap.values()).iterator(); iterator.hasNext(); ) {
                O next = iterator.next();

                if (next.getId().equals(id)) {
                    return next;
                }
            }
        }

        if (idFieldName == null) {
            return null;
        }

        AtomicReference<O> databaseObject = new AtomicReference<>();

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, idFieldName, id.toString()), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    databaseObject.set(loadObject(result));
                    break;
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });

        return databaseObject.get();
    }

    public void getDataFromDB(Object id, Consumer<O> consumer) {
        if (idFieldName == null) {
            consumer.accept(null);
            return;
        }

        hikariAPI.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s';", table, idFieldName, id.toString()), result -> {
            if (result == null) {
                consumer.accept(null);
                return;
            }

            try {
                while (result.next()) {
                    consumer.accept(loadObject(result));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void registerRedisHook(RedisClient redisClient) {
        this.redisClient = redisClient;
        redisClient.registerListener(new HikariUpdateRedisMessageListener<O>(this));
    }

}
