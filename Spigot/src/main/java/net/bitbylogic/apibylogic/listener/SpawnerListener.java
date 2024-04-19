package net.bitbylogic.apibylogic.listener;

import net.bitbylogic.apibylogic.util.ItemStackUtil;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        ItemStack item = event.getItemInHand();

        if (!ItemStackUtil.isSpawner(item)) {
            return;
        }

        EntityType entityType = ItemStackUtil.getSpawnerEntity(item);

        if (entityType == null) {
            return;
        }

        CreatureSpawner spawner = (CreatureSpawner) event.getBlock().getState();
        spawner.setSpawnedType(entityType);
        spawner.update(true, false);
    }

}
