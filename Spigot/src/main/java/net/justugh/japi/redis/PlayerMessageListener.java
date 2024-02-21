package net.justugh.japi.redis;

import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import net.justugh.japi.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerMessageListener extends RedisMessageListener {

    public PlayerMessageListener() {
        super("j-message");
    }

    @Override
    public void onReceive(ListenerComponent component) {
        Player player = Bukkit.getPlayer(component.getData("uuid", UUID.class));

        if (player == null) {
            return;
        }

        player.sendMessage(Format.format(component.getData("message", String.class)));
    }

}
