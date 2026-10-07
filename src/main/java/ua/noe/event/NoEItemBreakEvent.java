package ua.noe.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.noe.item.NoEItem;
import org.bukkit.event.HandlerList;

/** Fired when a NoE item is about to break from durability loss. Cancelling prevents the damage. */
public class NoEItemBreakEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NoEItem item;
    private final ItemStack itemStack;

    public NoEItemBreakEvent(Player player, NoEItem item, ItemStack itemStack) {
        this.player = player;
        this.item = item;
        this.itemStack = itemStack;
    }

    public Player getPlayer() {
        return player;
    }

    public NoEItem getItem() {
        return item;
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
