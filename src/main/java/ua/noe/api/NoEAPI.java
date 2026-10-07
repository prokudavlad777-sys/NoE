package ua.noe.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.noe.NoEPlugin;
import ua.noe.armor.NoEArmor;
import ua.noe.armor.ProtectionType;
import ua.noe.block.NoEBlock;
import ua.noe.food.NoEFood;
import ua.noe.item.NoEItem;
import ua.noe.weapon.NoEWeapon;

import java.util.Collection;

/**
 * Public API of NoE. All methods are safe to call from the main thread once NoE is enabled; declare
 * {@code depend: [NoE]} (or softdepend) in your plugin descriptor.
 *
 * <pre>{@code
 * ItemStack sword = NoEAPI.createItem("tiger_sword");
 * if (NoEAPI.isNoEItem(player.getInventory().getItemInMainHand())) { ... }
 * }</pre>
 */
public final class NoEAPI {

    private static volatile NoEPlugin plugin;

    private NoEAPI() {
    }

    public static void init(NoEPlugin instance) {
        plugin = instance;
    }

    public static void shutdown() {
        plugin = null;
    }

    private static NoEPlugin require() {
        NoEPlugin p = plugin;
        if (p == null) {
            throw new IllegalStateException("NoE is not enabled. Add NoE to your plugin's depend list.");
        }
        return p;
    }

    /** @return true while NoE is enabled */
    public static boolean isAvailable() {
        return plugin != null;
    }

    /** Definition by id, or {@code null}. */
    public static NoEItem getItem(String id) {
        return require().factory().registry().get(id);
    }

    /** Definition of an item stack, or {@code null} if it is not a NoE item. */
    public static NoEItem getItem(ItemStack stack) {
        String id = getItemId(stack);
        return id == null ? null : getItem(id);
    }

    /** Creates one item. Fires {@link ua.noe.event.NoEItemCreateEvent}; returns null for unknown ids or cancellation. */
    public static ItemStack createItem(String id) {
        return createItem(id, 1);
    }

    public static ItemStack createItem(String id, int amount) {
        return require().factory().create(id, amount, "api");
    }

    public static boolean isNoEItem(ItemStack stack) {
        return require().identifier().isNoEItem(stack);
    }

    /** The {@code noe:item_id} of a stack, or {@code null}. */
    public static String getItemId(ItemStack stack) {
        return require().identifier().getId(stack);
    }

    public static NoEWeapon getWeapon(String id) {
        return require().factory().registry().weapon(id).orElse(null);
    }

    public static NoEArmor getArmor(String id) {
        return require().factory().registry().armor(id).orElse(null);
    }

    public static NoEBlock getBlock(String id) {
        return require().factory().registry().block(id).orElse(null);
    }

    public static NoEFood getFood(String id) {
        return require().factory().registry().food(id).orElse(null);
    }

    /** The custom block definition at a world position, or {@code null} for ordinary blocks. */
    public static NoEBlock getCustomBlock(Block block) {
        String id = require().blocks().getId(block);
        return id == null ? null : getBlock(id);
    }

    /** Total protection (0.0 - configured maximum) the player's worn NoE armor gives against a damage type. */
    public static double getArmorProtection(Player player, ProtectionType type) {
        return require().armor().getProtection(player, type);
    }

    /** All registered ids, sorted. */
    public static Collection<String> getItemIds() {
        return require().factory().registry().ids();
    }
}
