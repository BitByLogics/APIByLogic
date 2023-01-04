package net.justugh.japi.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class ChatInputAction extends Action {

    private boolean cancel;

    public ChatInputAction(boolean cancel, TimeUnit unit, int time, int allowedActivations) {
        super(unit, time, allowedActivations);
        this.cancel = cancel;
    }

    public abstract void onTrigger(Player player, String message);

    @EventHandler
    public void onTrigger(AsyncPlayerChatEvent event) {
        if (onActivate()) {
            return;
        }

        event.setCancelled(cancel);
        onTrigger(event.getPlayer(), event.getMessage());
    }

}
