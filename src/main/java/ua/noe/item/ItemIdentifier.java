package ua.noe.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Reads the {@code noe:item_id} tag from item stacks. */
public final class ItemIdentifier {

    private final NamespacedKey idKey;

    public ItemIdentifier(NamespacedKey idKey) {
        this.idKey = idKey;
    }

    public NamespacedKey key() {
        return idKey;
    }

    /** @return the NoE id of the stack, or {@code null} if it is not a NoE item. */
    public String getId(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(idKey, PersistentDataType.STRING);
    }

    public boolean isNoEItem(ItemStack stack) {
        return getId(stack) != null;
    }
}
