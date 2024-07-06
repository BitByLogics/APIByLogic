package net.bitbylogic.apibylogic.database.hikari.redis;

import net.bitbylogic.apibylogic.database.hikari.data.HikariObject;
import net.bitbylogic.apibylogic.database.hikari.data.HikariTable;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.database.redis.listener.RedisMessageListener;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

        Optional<O> optionalObject = hikariTable.getDataById(objectId);

        if (optionalObject.isEmpty()) {
            return;
        }

        O object = optionalObject.get();

        try (ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor()) {
            service.schedule(() -> {
                switch (updateType) {
                    case SAVE:
                        hikariTable.getDataMap().remove(object.getId());

                        try (ScheduledExecutorService innerService = Executors.newSingleThreadScheduledExecutor()) {
                            innerService.execute(() -> {
                                hikariTable.getDataFromDB(objectId, o -> {
                                    o.ifPresent(obj -> {
                                        hikariTable.getDataMap().put(obj.getId(), obj);
                                    });
                                });
                            });
                        }
                        break;
                    case DELETE:
                        hikariTable.getDataMap().remove(object.getId());
                        break;
                    default:
                        break;
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

}
