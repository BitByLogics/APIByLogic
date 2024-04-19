package net.bitbylogic.apibylogic.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bitbylogic.apibylogic.APIByLogic;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class Action implements Listener {

    private long expireTime = -1;

    @Setter
    private int allowedActivations;

    public Action(TimeUnit unit, int time, int allowedActivations) {
        this.expireTime = time > 0 ? unit.toSeconds(time) * 20 : time;
        this.allowedActivations = allowedActivations;
    }

    public abstract void onExpire(@Nullable Player player);

    public boolean onActivate() {
        if (allowedActivations == -1) {
            return false;
        }

        if (allowedActivations == 0) {
            HandlerList.unregisterAll(this);
            return true;
        }

        allowedActivations--;
        return false;
    }

    public boolean isCompleted() {
        return allowedActivations == 0;
    }

    public void register() {
        Bukkit.getServer().getPluginManager().registerEvents(this, APIByLogic.getInstance());
    }

}
