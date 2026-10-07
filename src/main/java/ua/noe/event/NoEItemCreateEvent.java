package ua.noe.event;

import org.bukkit.inventory.ItemStack;
import ua.noe.item.NoEItem;
import org.bukkit.event.HandlerList;

/** Fired when NoE creates an item for an API call, command or GUI. Listeners may replace the stack or cancel. */
public class NoEItemCreateEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NoEItem item;
    private ItemStack itemStack;
    private final String reason;

    public NoEItemCreateEvent(NoEItem item, ItemStack itemStack, String reason) {
        this.item = item;
        this.itemStack = itemStack;
        this.reason = reason;
    }

    public NoEItem getItem() {
        return item;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getReason() {
        return reason;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
