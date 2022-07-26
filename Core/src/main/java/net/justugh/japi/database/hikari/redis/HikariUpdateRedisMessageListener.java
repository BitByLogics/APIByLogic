package net.justugh.japi.database.hikari.redis;

import net.justugh.japi.database.hikari.data.HikariObject;
import net.justugh.japi.database.hikari.data.HikariTable;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;

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

        switch (updateType) {
            case SAVE:
                if (object != null) {
                    hikariTable.getData().remove(object);
                }

                hikariTable.getDataFromDB(objectId, o -> {
                    hikariTable.getData().add(o);
                });
                break;
            case DELETE:
                if (object != null) {
                    hikariTable.getData().remove(object);
                }
                break;
            default:
                break;
        }
    }

}
