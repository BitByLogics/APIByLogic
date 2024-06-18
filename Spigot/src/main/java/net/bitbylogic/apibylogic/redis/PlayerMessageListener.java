package net.bitbylogic.apibylogic.redis;

import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.database.redis.listener.RedisMessageListener;
import net.bitbylogic.apibylogic.util.message.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerMessageListener extends RedisMessageListener {

    public PlayerMessageListener() {
        super("abl-message");
    }

    @Override
    public void onReceive(ListenerComponent component) {
        Player player = Bukkit.getPlayer(component.getData("uuid", UUID.class));

        if (player == null) {
            return;
        }

        player.sendMessage(Formatter.format(component.getData("message", String.class)));
    }

}
