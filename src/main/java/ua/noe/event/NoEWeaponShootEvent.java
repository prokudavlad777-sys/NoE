package ua.noe.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.noe.weapon.NoEWeapon;
import org.bukkit.event.HandlerList;

/** Fired before a NoE weapon shoots. Cancelling prevents the shot and ammo use. */
public class NoEWeaponShootEvent extends NoECancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NoEWeapon weapon;
    private final ItemStack itemStack;

    public NoEWeaponShootEvent(Player player, NoEWeapon weapon, ItemStack itemStack) {
        this.player = player;
        this.weapon = weapon;
        this.itemStack = itemStack;
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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
