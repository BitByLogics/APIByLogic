package net.justugh.japi.action;

import lombok.Setter;
import net.justugh.japi.JustAPIPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper class for Action, supports events that have item methods.
 * Current supported events are listed below:
 * - PlayerInteractEvent
 * - PlayerInteractAtEntityEvent
 *
 * @param <E> Event to listen to
 */
@Setter
public abstract class ItemAction<E extends Event> extends Action<E> {

    private String itemIdentifier;

    public ItemAction() {
        super(TimeUnit.SECONDS, -1, -1);
    }

    public ItemAction(int allowedActivations) {
        super(TimeUnit.SECONDS, -1, allowedActivations);
    }

    public ItemAction(TimeUnit unit, int time, int allowedActivations) {
        super(unit, time, allowedActivations);
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }

        return itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(JustAPIPlugin.getInstance(), itemIdentifier), PersistentDataType.STRING);
    }

    @EventHandler
    public void superTrigger(E event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent castedEvent = (PlayerInteractEvent) event;

            if (!matches(castedEvent.getItem())) {
                return;
            }

            onTrigger(event);
        }

        if (event instanceof PlayerInteractAtEntityEvent) {
            PlayerInteractAtEntityEvent castedEvent = (PlayerInteractAtEntityEvent) event;

            if (!matches(castedEvent.getPlayer().getInventory().getItemInMainHand())
                    && !matches(castedEvent.getPlayer().getInventory().getItemInOffHand())) {
                return;
            }

            onTrigger(event);
        }

        if (getAllowedActivations() == -1) {
            return;
        }

        if (getAllowedActivations() == 0) {
            HandlerList.unregisterAll(this);
            return;
        }

        setAllowedActivations(getAllowedActivations() - 1);
    }

}
