package ua.noe.item;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import ua.noe.NoEPlugin;
import ua.noe.event.NoEItemBreakEvent;
import ua.noe.event.NoEItemUseEvent;
import ua.noe.weapon.NoEWeapon;

/** Generic NoE item behaviour: use events, weapon dispatch and durability. */
public final class ItemListener implements Listener {

    private final NoEPlugin plugin;

    public ItemListener(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL
                || event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        ItemStack stack = event.getItem();
        String id = plugin.identifier().getId(stack);
        if (id == null) {
            return;
        }
        NoEItem def = plugin.factory().registry().get(id);
        if (def == null) {
            return;
        }
        Player player = event.getPlayer();
        NoEItemUseEvent use = new NoEItemUseEvent(player, def, stack, event.getAction(), event.getHand(),
                event.getClickedBlock());
        plugin.getServer().getPluginManager().callEvent(use);
        if (use.isCancelled()) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }
        if (def instanceof NoEWeapon weapon && weapon.isRanged()
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setUseItemInHand(Event.Result.DENY);
            Block clicked = event.getClickedBlock();
            if (clicked != null && clicked.getType().isInteractable() && !player.isSneaking()) {
                return; // let the player open the chest / door instead of shooting
            }
            plugin.weapons().shoot(player, weapon);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        String id = plugin.identifier().getId(player.getInventory().getItemInMainHand());
        if (id != null && plugin.factory().registry().get(id) instanceof NoEWeapon weapon && weapon.isRanged()) {
            event.setCancelled(true);
            plugin.weapons().startReload(player, weapon);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack stack = event.getItem();
        String id = plugin.identifier().getId(stack);
        if (id == null) {
            return;
        }
        NoEItem def = plugin.factory().registry().get(id);
        ItemMeta meta = stack.getItemMeta();
        if (def == null || !(meta instanceof Damageable damageable)) {
            return;
        }
        int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : stack.getType().getMaxDurability();
        if (max > 0 && damageable.getDamage() + event.getDamage() >= max) {
            NoEItemBreakEvent breakEvent = new NoEItemBreakEvent(event.getPlayer(), def, stack);
            plugin.getServer().getPluginManager().callEvent(breakEvent);
            if (breakEvent.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        plugin.weapons().cancelReload(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.weapons().cancelReload(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.weapons().cancelReload(event.getPlayer());
    }
}
