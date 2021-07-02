package net.justugh.japi.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

@Getter
@NoArgsConstructor
public abstract class Action {

    private long expireTime = -1;

    @Setter
    private boolean completed;

    public Action(TimeUnit unit, int time) {
        expireTime = unit.toSeconds(time) * 20;
    }

    public abstract void onExpire(Player player);

}
