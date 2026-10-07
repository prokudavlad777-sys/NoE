package ua.noe.armor;

import org.bukkit.inventory.EquipmentSlot;
import ua.noe.item.ItemData;
import ua.noe.item.NoEItem;

/** A custom armor piece with per-damage-type protection (0.0 - 1.0 damage reduction). */
public class NoEArmor extends NoEItem {

    private final EquipmentSlot slot;
    private final double meleeProtection;
    private final double firearmProtection;

    public NoEArmor(ItemData data, EquipmentSlot slot, double meleeProtection, double firearmProtection) {
        super(data);
        this.slot = slot;
        this.meleeProtection = meleeProtection;
        this.firearmProtection = firearmProtection;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public double getMeleeProtection() {
        return meleeProtection;
    }

    public double getFirearmProtection() {
        return firearmProtection;
    }
}
