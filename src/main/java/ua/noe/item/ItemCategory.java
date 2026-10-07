package ua.noe.item;

import java.util.Locale;

/** Kind of NoE definition; the config folder name decides the category. */
public enum ItemCategory {
    ITEM("items"),
    WEAPON("weapons"),
    ARMOR("armor"),
    BLOCK("blocks"),
    FOOD("food");

    private final String folder;

    ItemCategory(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }

    public String displayName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
