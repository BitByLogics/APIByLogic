package net.justugh.japi.action;

import lombok.Getter;
import lombok.Setter;
import net.justugh.japi.JustAPIPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

@Getter @Setter
public abstract class ItemInteractAction {

    private String itemIdentifier;

    public abstract void onTrigger(PlayerInteractEvent event);

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }

        return itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(JustAPIPlugin.getInstance(), itemIdentifier), PersistentDataType.STRING);
    }

}
