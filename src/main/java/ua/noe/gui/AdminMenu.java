package ua.noe.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.noe.NoEPlugin;
import ua.noe.command.CommandArgs;
import ua.noe.item.NoEItem;
import ua.noe.util.Text;

import java.util.ArrayList;
import java.util.List;

/** Builds the paged, searchable item browser opened by /noe menu. */
public final class AdminMenu {

    public static final int ITEMS_PER_PAGE = 45;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_SEARCH = 47;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_RELOAD = 51;
    public static final int SLOT_NEXT = 53;

    private final NoEPlugin plugin;

    public AdminMenu(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String query, int page) {
        List<String> matches = plugin.factory().registry().search(query, item ->
                item.getName() == null ? null : Text.stripColors(item.getName()));
        int pages = CommandArgs.pageCount(matches.size(), ITEMS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        int from = safePage * ITEMS_PER_PAGE;
        List<String> pageIds = new ArrayList<>(matches.subList(from, Math.min(matches.size(), from + ITEMS_PER_PAGE)));

        Component title = Text.color("&6NoE &8- &7" + matches.size() + " items" + (query.isBlank() ? "" : " &8| &7" + query));
        MenuHolder holder = new MenuHolder(title, query, safePage, pages, pageIds);
        Inventory inv = holder.getInventory();

        for (int slot = 0; slot < pageIds.size(); slot++) {
            ItemStack display = plugin.factory().createSilent(pageIds.get(slot), 1);
            if (display == null) {
                continue;
            }
            NoEItem def = plugin.factory().registry().get(pageIds.get(slot));
            display.editMeta(meta -> {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(Component.empty());
                lore.add(Text.item("&8id: &7" + def.getId() + " &8[" + def.getCategory().displayName() + "]"));
                lore.add(Text.item("&eLeft-click: &7take 1  &eShift: &7take stack"));
                lore.add(Text.item("&eRight-click: &7show stats"));
                meta.lore(lore);
            });
            inv.setItem(slot, display);
        }

        inv.setItem(SLOT_PREV, button(Material.ARROW, "&ePrevious page"));
        inv.setItem(SLOT_SEARCH, button(Material.COMPASS, "&bSearch"));
        inv.setItem(SLOT_INFO, button(Material.PAPER, "&7Page &f" + (safePage + 1) + "&7/&f" + pages));
        inv.setItem(SLOT_RELOAD, button(Material.REDSTONE, "&cReload NoE"));
        inv.setItem(SLOT_NEXT, button(Material.ARROW, "&eNext page"));
        player.openInventory(inv);
    }

    private static ItemStack button(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(m -> m.displayName(Text.item(name)));
        return stack;
    }
}
