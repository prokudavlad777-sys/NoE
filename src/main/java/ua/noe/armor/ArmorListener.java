package ua.noe.armor;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ua.noe.NoEPlugin;
import ua.noe.event.NoEArmorEquipEvent;
import ua.noe.item.NoEItem;

import java.util.HashMap;
import java.util.Map;

/** Equip events and damage reduction for NoE armor. */
public final class ArmorListener implements Listener {

    private final NoEPlugin plugin;

    public ArmorListener(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        ProtectionType type = classify(event);
        if (type == null) {
            return;
        }
        double reduction = plugin.armor().getProtection(victim, type);
        if (reduction > 0.0) {
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }
    }

    private ProtectionType classify(EntityDamageByEntityEvent event) {
        if (plugin.damageContext().isFirearm()) {
            return ProtectionType.FIREARM;
        }
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile
                && projectile.getPersistentDataContainer().has(plugin.keys().projectile, PersistentDataType.BYTE)) {
            return ProtectionType.FIREARM;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return ProtectionType.MELEE;
        }
        return null;
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        ItemStack newItem = event.getNewItem();
        String id = plugin.identifier().getId(newItem);
        if (id == null || !(plugin.factory().registry().get(id) instanceof NoEArmor armor)) {
            return;
        }
        Player player = event.getPlayer();
        EquipmentSlot slot = event.getSlotType().getSlot();
        NoEArmorEquipEvent equip = new NoEArmorEquipEvent(player, armor, slot, newItem);
        plugin.getServer().getPluginManager().callEvent(equip);
        if (!equip.isCancelled()) {
            return;
        }
        // Return the piece to the inventory next tick; never touch the old item, it is already elsewhere.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack worn = player.getInventory().getItem(slot);
            if (worn.getType().isAir() || !armor.getId().equals(plugin.identifier().getId(worn))) {
                return;
            }
            player.getInventory().setItem(slot, null);
            Map<Integer, ItemStack> leftovers = new HashMap<>(player.getInventory().addItem(worn));
            leftovers.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            plugin.messages().send(player, "armor-cancelled");
        });
    }

    /** Looks up a definition for an item the player wears; used by API consumers. */
    public NoEItem definitionOf(ItemStack stack) {
        String id = plugin.identifier().getId(stack);
        return id == null ? null : plugin.factory().registry().get(id);
    }
}
