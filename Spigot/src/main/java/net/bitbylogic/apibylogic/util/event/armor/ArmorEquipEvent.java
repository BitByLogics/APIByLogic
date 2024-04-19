package net.bitbylogic.apibylogic.util.event.armor;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

// Original Credit: https://github.com/Arnuh/ArmorEquipEvent
@Getter
@Setter
public class ArmorEquipEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ArmorEquipMethod equipType;
    private final ArmorType armorType;

    private ItemStack oldArmorPiece, newArmorPiece;
    private boolean cancelled = false;

    /**
     * @param player        The player who put on / removed the armor.
     * @param armorType     The ArmorType of the armor added
     * @param oldArmorPiece The ItemStack of the armor removed.
     * @param newArmorPiece The ItemStack of the armor added.
     */
    public ArmorEquipEvent(Player player, ArmorEquipMethod equipType, ArmorType armorType, ItemStack oldArmorPiece, ItemStack newArmorPiece) {
        this.player = player;
        this.equipType = equipType;
        this.armorType = armorType;
        this.oldArmorPiece = oldArmorPiece;
        this.newArmorPiece = newArmorPiece;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}