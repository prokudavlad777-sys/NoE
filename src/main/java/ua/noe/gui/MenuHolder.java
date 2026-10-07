package ua.noe.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** Identifies a NoE admin menu inventory and remembers what it shows. */
public final class MenuHolder implements InventoryHolder {

    private final Inventory inventory;
    private final String query;
    private final int page;
    private final int pages;
    private final List<String> pageIds;

    public MenuHolder(Component title, String query, int page, int pages, List<String> pageIds) {
        this.inventory = Bukkit.createInventory(this, 54, title);
        this.query = query;
        this.page = page;
        this.pages = pages;
        this.pageIds = pageIds;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String query() {
        return query;
    }

    public int page() {
        return page;
    }

    public int pages() {
        return pages;
    }

    /** Item id at an inventory slot of the current page, or null. */
    public String idAt(int slot) {
        return slot >= 0 && slot < pageIds.size() ? pageIds.get(slot) : null;
    }
}
