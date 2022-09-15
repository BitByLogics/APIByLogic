package net.justugh.japi.action;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public abstract class PlayerInteractAction extends ItemAction<PlayerInteractEvent> {

    @Override
    public void onExpire(@Nullable Player player) {

    }

    public abstract void onClick(PlayerInteractEvent event);

    @Override
    public void onTrigger(PlayerInteractEvent event) {
        onClick(event);
    }

}
