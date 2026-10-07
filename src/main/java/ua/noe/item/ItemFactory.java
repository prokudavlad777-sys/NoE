package ua.noe.item;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import ua.noe.config.ConfigReport;
import ua.noe.event.NoEItemCreateEvent;
import ua.noe.registry.ItemRegistry;
import ua.noe.util.Keys;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds each NoE item once and hands out clones. Prototypes are rebuilt only when the registry reloads,
 * so normal operation never rebuilds item meta.
 */
public final class ItemFactory {

    private final Keys keys;
    private volatile ItemRegistry registry = ItemRegistry.EMPTY;
    private volatile Map<String, ItemStack> prototypes = Map.of();

    public ItemFactory(Keys keys) {
        this.keys = keys;
    }

    /** Replaces the registry and rebuilds all prototypes. Must run on the main thread. */
    public void rebuild(ItemRegistry newRegistry, ConfigReport report) {
        Map<String, ItemStack> built = new HashMap<>();
        for (NoEItem item : newRegistry.all()) {
            try {
                built.put(item.getId(), ItemBuilder.fromDefinition(item, keys, report).build());
            } catch (RuntimeException ex) {
                report.error(item.getSource(), item.getId(), null,
                        "Could not build the item stack: " + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        "Check material and component settings for this item");
            }
        }
        this.registry = newRegistry;
        this.prototypes = Map.copyOf(built);
    }

    public ItemRegistry registry() {
        return registry;
    }

    /** A copy of the cached prototype, or {@code null} for unknown ids. No event is fired. */
    public ItemStack createSilent(String id, int amount) {
        ItemStack proto = prototypes.get(id);
        if (proto == null) {
            return null;
        }
        ItemStack copy = proto.clone();
        copy.setAmount(Math.max(1, Math.min(amount, copy.getMaxStackSize())));
        return copy;
    }

    /**
     * Creates an item and fires {@link NoEItemCreateEvent}.
     *
     * @return the stack, or {@code null} if the id is unknown or an event listener cancelled creation
     */
    public ItemStack create(String id, int amount, String reason) {
        ItemStack stack = createSilent(id, amount);
        NoEItem def = registry.get(id);
        if (stack == null || def == null) {
            return null;
        }
        NoEItemCreateEvent event = new NoEItemCreateEvent(def, stack, reason);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled() ? null : event.getItemStack();
    }
}
