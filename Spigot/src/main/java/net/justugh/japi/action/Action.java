package net.justugh.japi.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class Action<E extends Event> implements Listener {

    private long expireTime = -1;

    @Setter
    private int allowedActivations;

    public Action(TimeUnit unit, int time, int allowedActivations) {
        this.expireTime = time > 0 ? unit.toSeconds(time) * 20 : time;
        this.allowedActivations = allowedActivations;
    }

    public abstract void onExpire(@Nullable Player player);

    public abstract void onTrigger(E event);

    @EventHandler
    public void superTrigger(E event) {
        onTrigger(event);

        if (allowedActivations == -1) {
            return;
        }

        if (allowedActivations == 0) {
            HandlerList.unregisterAll(this);
            return;
        }

        allowedActivations--;
    }

    public boolean isCompleted() {
        return allowedActivations == 0;
    }

}
