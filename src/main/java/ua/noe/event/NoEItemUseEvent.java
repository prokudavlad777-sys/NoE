package ua.noe.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ua.noe.item.NoEItem;
import org.bukkit.event.HandlerList;

/** Fired when a player clicks while holding a NoE item. Cancelling denies the interaction. */
public class NoEItemUseEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NoEItem item;
    private final ItemStack itemStack;
    private final Action action;
    private final EquipmentSlot hand;
    private final Block clickedBlock;

    public NoEItemUseEvent(Player player, NoEItem item, ItemStack itemStack, Action action, EquipmentSlot hand, Block clickedBlock) {
        this.player = player;
        this.item = item;
        this.itemStack = itemStack;
        this.action = action;
        this.hand = hand;
        this.clickedBlock = clickedBlock;
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

    public Action getAction() {
        return action;
    }

    public EquipmentSlot getHand() {
        return hand;
    }

    public Block getClickedBlock() {
        return clickedBlock;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
