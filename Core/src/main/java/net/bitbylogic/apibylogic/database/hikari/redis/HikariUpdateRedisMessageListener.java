package net.bitbylogic.apibylogic.database.hikari.redis;

import net.bitbylogic.apibylogic.database.hikari.data.HikariObject;
import net.bitbylogic.apibylogic.database.hikari.data.HikariTable;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.database.redis.listener.RedisMessageListener;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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

        Executor delayedExecutor = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

        CompletableFuture.runAsync(() -> {
            switch (updateType) {
                case SAVE:
                    hikariTable.getDataMap().remove(hikariTable.getStatements().getId(object));

                    hikariTable.getDataFromDB(objectId, optional ->
                            optional.ifPresent(result -> hikariTable.getDataMap().put(hikariTable.getStatements().getId(result), result)));
                    break;
                case DELETE:
                    hikariTable.getDataMap().remove(hikariTable.getStatements().getId(object));
                    break;
                default:
                    break;
            }
        }, delayedExecutor);
    }

}
