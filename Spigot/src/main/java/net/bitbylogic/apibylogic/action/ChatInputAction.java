package net.bitbylogic.apibylogic.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class ChatInputAction extends Action {

    private UUID uuid;
    private boolean cancel;

    public ChatInputAction(UUID uuid, boolean cancel, TimeUnit unit, int time, int allowedActivations) {
        super(unit, time, allowedActivations);
        this.uuid = uuid;
        this.cancel = cancel;
    }

    public abstract void onTrigger(Player player, String message);

    @EventHandler
    public void onTrigger(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(uuid)) {
            return;
        }

        if (onActivate()) {
            return;
        }

        event.setCancelled(cancel);
        onTrigger(event.getPlayer(), event.getMessage());
    }

}
