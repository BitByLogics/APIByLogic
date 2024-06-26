package net.bitbylogic.apibylogic.database.hikari.redis;

import net.bitbylogic.apibylogic.database.hikari.data.HikariObject;
import net.bitbylogic.apibylogic.database.hikari.data.HikariTable;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.database.redis.listener.RedisMessageListener;

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
                        hikariTable.getDataMap().remove(object.getId());
                    }

                    Executors.newSingleThreadScheduledExecutor().execute(() -> {
                        hikariTable.getDataFromDB(objectId, o -> {
                            hikariTable.getDataMap().put(o.getId(), o);
                        });
                    });
                    break;
                case DELETE:
                    if (object != null) {
                        hikariTable.getDataMap().remove(object.getId());
                    }
                    break;
                default:
                    break;
            }
        }, 1, TimeUnit.SECONDS);
    }

}
