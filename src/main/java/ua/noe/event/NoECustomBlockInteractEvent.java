package ua.noe.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import ua.noe.block.NoEBlock;
import org.bukkit.event.HandlerList;

/** Fired when a player right-clicks a NoE custom block. Cancelling blocks all interaction. */
public class NoECustomBlockInteractEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NoEBlock definition;
    private final Block block;

    public NoECustomBlockInteractEvent(Player player, NoEBlock definition, Block block) {
        this.player = player;
        this.definition = definition;
        this.block = block;
    }

    public Player getPlayer() {
        return player;
    }

    public NoEBlock getDefinition() {
        return definition;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
