package ua.noe.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** All namespaced keys used by NoE. The namespace is always {@code noe}. */
public final class Keys {

    /** {@code noe:item_id} - the NoE id stored in every NoE item. */
    public final NamespacedKey itemId;
    /** {@code noe:ammo} - rounds currently in a weapon's magazine. */
    public final NamespacedKey ammo;
    /** {@code noe:flags} - comma separated custom flags. */
    public final NamespacedKey flags;
    /** {@code noe:blocks} - custom block positions stored in chunk data. */
    public final NamespacedKey blocks;
    /** {@code noe:block_display} - marks display entities of custom blocks. */
    public final NamespacedKey blockDisplay;
    /** {@code noe:projectile} - marks projectiles fired by NoE weapons. */
    public final NamespacedKey projectile;

    private final Plugin plugin;

    public Keys(Plugin plugin) {
        this.plugin = plugin;
        this.itemId = new NamespacedKey(plugin, "item_id");
        this.ammo = new NamespacedKey(plugin, "ammo");
        this.flags = new NamespacedKey(plugin, "flags");
        this.blocks = new NamespacedKey(plugin, "blocks");
        this.blockDisplay = new NamespacedKey(plugin, "block_display");
        this.projectile = new NamespacedKey(plugin, "projectile");
    }

    /** Key for an attribute modifier belonging to a NoE item. */
    public NamespacedKey modifier(String itemId, String suffix) {
        return new NamespacedKey(plugin, itemId + "_" + suffix);
    }
}
