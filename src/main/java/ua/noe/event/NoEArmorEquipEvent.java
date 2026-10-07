package ua.noe.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ua.noe.armor.NoEArmor;
import org.bukkit.event.HandlerList;

/** Fired after a NoE armor piece was equipped. Cancelling moves it back to the inventory. */
public class NoEArmorEquipEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NoEArmor armor;
    private final EquipmentSlot slot;
    private final ItemStack itemStack;

    public NoEArmorEquipEvent(Player player, NoEArmor armor, EquipmentSlot slot, ItemStack itemStack) {
        this.player = player;
        this.armor = armor;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public Player getPlayer() {
        return player;
    }

    public NoEArmor getArmor() {
        return armor;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
