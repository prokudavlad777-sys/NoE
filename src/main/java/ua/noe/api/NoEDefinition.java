package ua.noe.api;

import org.bukkit.Material;
import ua.noe.item.ItemCategory;

/** Minimal read-only view of any NoE definition. Safe for other plugins to depend on. */
public interface NoEDefinition {

    /** Unique id, e.g. {@code tiger_sword}. */
    String getId();

    ItemCategory getCategory();

    Material getMaterial();
}
