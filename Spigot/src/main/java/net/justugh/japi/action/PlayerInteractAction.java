package net.justugh.japi.action;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.JustAPIPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class PlayerInteractAction extends Action {

    private String itemIdentifier;

    @Override
    public void onExpire(@Nullable Player player) {

    }

    public abstract void onClick(PlayerInteractEvent event);

    @EventHandler
    public void onTrigger(PlayerInteractEvent event) {
        if(event.getItem() == null || !matches(event.getItem())) {
            return;
        }

        onClick(event);
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

}
