package net.justugh.japi.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class ChatInputAction extends Action {

    private boolean cancel;

    public ChatInputAction(boolean cancel, TimeUnit unit, int time) {
        super(unit, time);
        this.cancel = cancel;
    }

    public abstract void onTrigger(Player player, String message);

}
