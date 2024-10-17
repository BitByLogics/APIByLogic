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

    private UUID identifier;
    private boolean cancel;

    public ChatInputAction(UUID identifier, boolean cancel, TimeUnit unit, int time, int allowedActivations) {
        super(unit, time, allowedActivations);
        this.identifier = identifier;
        this.cancel = cancel;
    }

    public abstract void onTrigger(Player player, String message);

    @EventHandler
    public void onTrigger(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(identifier)) {
            return;
        }

        if (onActivate()) {
            return;
        }

        event.setCancelled(cancel);
        onTrigger(event.getPlayer(), event.getMessage());
    }

}
