package ua.noe.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.ClickType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import ua.noe.NoEPlugin;
import ua.noe.item.NoEItem;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Click handling for the admin menu and the chat-based search prompt. */
public final class MenuListener implements Listener {

    private final NoEPlugin plugin;
    private final Set<UUID> awaitingSearch = ConcurrentHashMap.newKeySet();

    public MenuListener(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        switch (slot) {
            case AdminMenu.SLOT_PREV -> plugin.menu().open(player, holder.query(), holder.page() - 1);
            case AdminMenu.SLOT_NEXT -> plugin.menu().open(player, holder.query(), holder.page() + 1);
            case AdminMenu.SLOT_SEARCH -> {
                awaitingSearch.add(player.getUniqueId());
                player.closeInventory();
                plugin.messages().send(player, "menu-search-prompt");
            }
            case AdminMenu.SLOT_RELOAD -> {
                if (!allowed(player, "noe.reload")) {
                    plugin.messages().send(player, "no-permission");
                    return;
                }
                plugin.reload().thenAccept(r -> plugin.menu().open(player, holder.query(), holder.page()));
            }
            default -> clickItem(player, holder.idAt(slot), event.getClick());
        }
    }

    private void clickItem(Player player, String id, ClickType click) {
        if (id == null) {
            return;
        }
        NoEItem def = plugin.factory().registry().get(id);
        if (def == null) {
            return;
        }
        if (click.isRightClick()) {
            if (!allowed(player, "noe.info")) {
                plugin.messages().send(player, "no-permission");
                return;
            }
            plugin.command().sendInfo(player, def);
            return;
        }
        if (!allowed(player, "noe.give")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        int amount = click.isShiftClick() ? 64 : 1;
        ItemStack stack = plugin.factory().create(id, amount, "gui");
        if (stack != null) {
            player.getInventory().addItem(stack).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingSearch.remove(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancel")) {
                plugin.messages().send(player, "menu-search-cancelled");
                return;
            }
            plugin.menu().open(player, text, 0);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaitingSearch.remove(event.getPlayer().getUniqueId());
    }

    private static boolean allowed(Player player, String permission) {
        return player.hasPermission(permission) || player.hasPermission("noe.admin");
    }
}
