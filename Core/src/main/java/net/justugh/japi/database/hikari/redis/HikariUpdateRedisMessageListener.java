package net.justugh.japi.database.hikari.redis;

import net.justugh.japi.database.hikari.data.HikariObject;
import net.justugh.japi.database.hikari.data.HikariTable;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HikariUpdateRedisMessageListener<O extends HikariObject> extends RedisMessageListener {

    private final HikariTable<O> hikariTable;

    public HikariUpdateRedisMessageListener(HikariTable<O> hikariTable) {
        super("hikari-update");
        this.hikariTable = hikariTable;
    }

    @Override
    public void onReceive(ListenerComponent component) {
        HikariRedisUpdateType updateType = component.getData("updateType", HikariRedisUpdateType.class);
        String objectId = component.getData("objectId", String.class);

        O object = hikariTable.getDataById(objectId);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            switch (updateType) {
                case SAVE:
                    if (object != null) {
                        hikariTable.getDataMap().remove(object.getDataId());
                    }

                    Executors.newSingleThreadScheduledExecutor().execute(() -> {
                        hikariTable.getDataFromDB(objectId, o -> {
                            hikariTable.getDataMap().put(o.getDataId(), o);
                        });
                    });
                    break;
                case DELETE:
                    if (object != null) {
                        hikariTable.getDataMap().remove(object.getDataId());
                    }
                    break;
                default:
                    break;
            }
        }, 1, TimeUnit.SECONDS);
    }

}
