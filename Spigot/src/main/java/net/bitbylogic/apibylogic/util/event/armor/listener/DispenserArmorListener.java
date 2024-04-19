package net.bitbylogic.apibylogic.util.event.armor.listener;

import net.bitbylogic.apibylogic.util.event.armor.ArmorType;
import net.bitbylogic.apibylogic.util.event.armor.ArmorEquipEvent;
import net.bitbylogic.apibylogic.util.event.armor.ArmorEquipMethod;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;

// Original Credit: https://github.com/Arnuh/ArmorEquipEvent
public class DispenserArmorListener implements Listener {


    @EventHandler
    public void dispenseArmorEvent(BlockDispenseArmorEvent event) {
        ArmorType type = ArmorType.matchType(event.getItem());
        if (type != null) {
            if (event.getTargetEntity() instanceof Player) {
                Player p = (Player) event.getTargetEntity();
                ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipMethod.DISPENSER, type, null, event.getItem());
                Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                if (armorEquipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}