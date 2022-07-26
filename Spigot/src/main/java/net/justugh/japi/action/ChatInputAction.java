package net.justugh.japi.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class ChatInputAction extends Action<AsyncPlayerChatEvent> {

    private boolean cancel;

    public ChatInputAction(boolean cancel, TimeUnit unit, int time, int allowedActivations) {
        super(unit, time, allowedActivations);
        this.cancel = cancel;
    }

    public abstract void onTrigger(Player player, String message);

    @Override
    public void onTrigger(AsyncPlayerChatEvent event) {
        event.setCancelled(cancel);
        onTrigger(event.getPlayer(), event.getMessage());
    }

}
