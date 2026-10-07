package ua.noe.armor;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ua.noe.NoEPlugin;
import ua.noe.item.NoEItem;

/** Computes the protection of everything a player wears. */
public final class ArmorService {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final NoEPlugin plugin;

    public ArmorService(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    /** Total damage reduction of worn NoE armor, 0.0 - configured maximum. */
    public double getProtection(Player player, ProtectionType type) {
        double sum = 0.0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getInventory().getItem(slot);
            String id = plugin.identifier().getId(stack);
            if (id == null) {
                continue;
            }
            NoEItem def = plugin.factory().registry().get(id);
            if (def instanceof NoEArmor armor && armor.getSlot() == slot) {
                sum += type == ProtectionType.MELEE ? armor.getMeleeProtection() : armor.getFirearmProtection();
            }
        }
        return Math.min(sum, plugin.settings().maxArmorProtection());
    }
}
