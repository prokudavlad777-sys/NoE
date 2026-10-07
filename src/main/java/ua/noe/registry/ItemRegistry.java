package ua.noe.registry;

import ua.noe.armor.NoEArmor;
import ua.noe.block.NoEBlock;
import ua.noe.config.ConfigReport;
import ua.noe.food.NoEFood;
import ua.noe.item.ItemCategory;
import ua.noe.item.NoEItem;
import ua.noe.weapon.NoEWeapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable registry of all NoE definitions. A reload builds a new registry and swaps the reference
 * in the plugin, so readers never see a half-loaded state.
 */
public final class ItemRegistry {

    public static final ItemRegistry EMPTY = new ItemRegistry(Map.of());

    private final Map<String, NoEItem> items;
    private final List<String> sortedIds;
    private final Map<ItemCategory, Integer> counts;

    private ItemRegistry(Map<String, NoEItem> items) {
        this.items = Collections.unmodifiableMap(items);
        List<String> ids = new ArrayList<>(items.keySet());
        Collections.sort(ids);
        this.sortedIds = Collections.unmodifiableList(ids);
        Map<ItemCategory, Integer> c = new EnumMap<>(ItemCategory.class);
        for (ItemCategory cat : ItemCategory.values()) {
            c.put(cat, 0);
        }
        for (NoEItem item : items.values()) {
            c.merge(item.getCategory(), 1, Integer::sum);
        }
        this.counts = Collections.unmodifiableMap(c);
    }

    public static Builder builder(ConfigReport report) {
        return new Builder(report);
    }

    public NoEItem get(String id) {
        return id == null ? null : items.get(id);
    }

    public boolean contains(String id) {
        return id != null && items.containsKey(id);
    }

    public Optional<NoEWeapon> weapon(String id) {
        return get(id) instanceof NoEWeapon w ? Optional.of(w) : Optional.empty();
    }

    public Optional<NoEArmor> armor(String id) {
        return get(id) instanceof NoEArmor a ? Optional.of(a) : Optional.empty();
    }

    public Optional<NoEBlock> block(String id) {
        return get(id) instanceof NoEBlock b ? Optional.of(b) : Optional.empty();
    }

    public Optional<NoEFood> food(String id) {
        return get(id) instanceof NoEFood f ? Optional.of(f) : Optional.empty();
    }

    /** All ids, sorted alphabetically. */
    public List<String> ids() {
        return sortedIds;
    }

    public Collection<NoEItem> all() {
        return items.values();
    }

    public int size() {
        return items.size();
    }

    public int count(ItemCategory category) {
        return counts.get(category);
    }

    /** Case-insensitive search over id and stripped display name. */
    public List<String> search(String query, java.util.function.Function<NoEItem, String> plainName) {
        String q = query.toLowerCase(java.util.Locale.ROOT).trim();
        if (q.isEmpty()) {
            return sortedIds;
        }
        List<String> out = new ArrayList<>();
        for (String id : sortedIds) {
            if (id.contains(q)) {
                out.add(id);
                continue;
            }
            String name = plainName.apply(items.get(id));
            if (name != null && name.toLowerCase(java.util.Locale.ROOT).contains(q)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Collects definitions, rejecting duplicate ids. */
    public static final class Builder {

        private final ConfigReport report;
        private final Map<String, NoEItem> items = new LinkedHashMap<>();

        private Builder(ConfigReport report) {
            this.report = report;
        }

        public Builder add(NoEItem item) {
            NoEItem existing = items.get(item.getId());
            if (existing != null) {
                report.error(item.getSource(), item.getId(), "id",
                        "Duplicate id; already defined in " + existing.getSource(),
                        "Give every item a unique id. This definition was skipped");
                return this;
            }
            items.put(item.getId(), item);
            return this;
        }

        /** Warns about weapons whose ammo item does not exist. */
        public ItemRegistry build() {
            for (NoEItem item : items.values()) {
                if (item instanceof NoEWeapon w && w.getAmmo() != null && !items.containsKey(w.getAmmo())) {
                    report.warn(w.getSource(), w.getId(), "ammo",
                            "Ammo item '" + w.getAmmo() + "' does not exist; the weapon can never be reloaded",
                            "Create an item with id '" + w.getAmmo() + "' in items/");
                }
            }
            return new ItemRegistry(new LinkedHashMap<>(items));
        }
    }
}
