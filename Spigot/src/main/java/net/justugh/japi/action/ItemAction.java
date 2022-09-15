package net.justugh.japi.action;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.JustAPIPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper class for Action, supports events that have item methods.
 * Current supported events are listed below:
 * - PlayerInteractEvent
 * - PlayerInteractAtEntityEvent
 */
@Getter
@Setter
public abstract class ItemAction<E extends Event> implements Listener {

    private String itemIdentifier;
    private long expireTime = -1;
    private int allowedActivations;

    public ItemAction() {
        this(TimeUnit.SECONDS, -1, -1);
    }

    public ItemAction(int allowedActivations) {
        this(TimeUnit.SECONDS, -1, allowedActivations);
    }

    public ItemAction(TimeUnit unit, int time, int allowedActivations) {
        this.expireTime = time > 0 ? unit.toSeconds(time) * 20 : time;
        this.allowedActivations = allowedActivations;
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }

        if (itemStack.getItemMeta() == null) {
            return false;
        }

        return itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(JustAPIPlugin.getInstance(), itemIdentifier), PersistentDataType.STRING);
    }

    public abstract void onExpire(@Nullable Player player);

    public abstract void onTrigger(PlayerInteractEvent event);

    @EventHandler
    public void superTrigger(PlayerInteractEvent event) {
        if (!matches(event.getPlayer().getInventory().getItemInMainHand())
                && !matches(event.getPlayer().getInventory().getItemInOffHand())) {
            return;
        }

        onTrigger(event);
    }

    public boolean isCompleted() {
        return allowedActivations == 0;
    }

}
