package ua.noe.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.noe.weapon.NoEWeapon;
import org.bukkit.event.HandlerList;

/** Fired when a reload starts. Cancelling prevents the reload. */
public class NoEWeaponReloadEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NoEWeapon weapon;
    private final ItemStack itemStack;
    private final int roundsToLoad;

    public NoEWeaponReloadEvent(Player player, NoEWeapon weapon, ItemStack itemStack, int roundsToLoad) {
        this.player = player;
        this.weapon = weapon;
        this.itemStack = itemStack;
        this.roundsToLoad = roundsToLoad;
    }

    public Player getPlayer() {
        return player;
    }

    public NoEWeapon getWeapon() {
        return weapon;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getRoundsToLoad() {
        return roundsToLoad;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
