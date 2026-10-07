package ua.noe.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.noe.NoEPlugin;
import ua.noe.armor.NoEArmor;
import ua.noe.block.NoEBlock;
import ua.noe.food.NoEFood;
import ua.noe.item.NoEItem;
import ua.noe.util.Text;
import ua.noe.weapon.NoEWeapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/** The /noe command with tab completion. */
public final class NoECommand implements BasicCommand {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_GIVE = 64 * 36;
    private static final Map<String, String> SUBCOMMANDS = Map.of(
            "give", "noe.give",
            "list", "noe.list",
            "info", "noe.info",
            "reload", "noe.reload",
            "pack", "noe.pack",
            "menu", "noe.menu");

    private final NoEPlugin plugin;

    public NoECommand(NoEPlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean allowed(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("noe.admin");
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.messages().sendList(sender, "help");
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String permission = SUBCOMMANDS.get(sub);
        if (permission == null) {
            plugin.messages().send(sender, "unknown-subcommand");
            return;
        }
        if (!allowed(sender, permission)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        switch (sub) {
            case "give" -> give(sender, args);
            case "list" -> list(sender, args);
            case "info" -> info(sender, args);
            case "reload" -> reload(sender);
            case "pack" -> pack(sender, args);
            case "menu" -> menu(sender, args);
            default -> plugin.messages().send(sender, "unknown-subcommand");
        }
    }

    // ------------------------------------------------------------------ give

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.color("&cUsage: /noe give <player> <item> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "player-not-found", "player", args[1]);
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (!plugin.factory().registry().contains(id)) {
            plugin.messages().send(sender, "item-not-found", "item", args[2]);
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            OptionalInt parsed = CommandArgs.parseAmount(args[3], MAX_GIVE);
            if (parsed.isEmpty()) {
                plugin.messages().send(sender, "invalid-amount", "amount", args[3], "max", String.valueOf(MAX_GIVE));
                return;
            }
            amount = parsed.getAsInt();
        }
        giveItem(target, id, amount);
        plugin.messages().send(sender, "give-success", "amount", String.valueOf(amount), "item", id, "player", target.getName());
        plugin.messages().send(target, "give-received", "amount", String.valueOf(amount), "item", id);
    }

    /** Gives items to a player, splitting into stacks and dropping what does not fit. */
    public void giveItem(Player target, String id, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = plugin.factory().create(id, remaining, "command");
            if (stack == null) {
                return;
            }
            remaining -= stack.getAmount();
            target.getInventory().addItem(stack)
                    .values().forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }
    }

    // ------------------------------------------------------------------ list / info

    private void list(CommandSender sender, String[] args) {
        List<String> ids = plugin.factory().registry().ids();
        int pages = CommandArgs.pageCount(ids.size(), PAGE_SIZE);
        int page = Math.min(CommandArgs.parsePage(args.length > 1 ? args[1] : null), pages);
        plugin.messages().send(sender, "list-header", "page", String.valueOf(page), "pages", String.valueOf(pages),
                "total", String.valueOf(ids.size()));
        int from = (page - 1) * PAGE_SIZE;
        for (int i = from; i < Math.min(ids.size(), from + PAGE_SIZE); i++) {
            NoEItem item = plugin.factory().registry().get(ids.get(i));
            sender.sendMessage(Text.color(plugin.messages().raw("list-entry", "id", item.getId(),
                    "category", item.getCategory().displayName())));
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.color("&cUsage: /noe info <item>"));
            return;
        }
        NoEItem item = plugin.factory().registry().get(args[1].toLowerCase(Locale.ROOT));
        if (item == null) {
            plugin.messages().send(sender, "item-not-found", "item", args[1]);
            return;
        }
        sendInfo(sender, item);
    }

    /** Prints every characteristic of an item; also used by the GUI. */
    public void sendInfo(CommandSender sender, NoEItem item) {
        plugin.messages().send(sender, "info-header", "id", item.getId());
        for (Map.Entry<String, String> line : describe(item).entrySet()) {
            sender.sendMessage(Text.color(plugin.messages().raw("info-line", "key", line.getKey(), "value", line.getValue())));
        }
    }

    public Map<String, String> describe(NoEItem item) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("category", item.getCategory().displayName());
        m.put("material", item.getMaterial().name());
        if (item.getName() != null) {
            m.put("name", Text.stripColors(item.getName()));
        }
        if (item.getModelData() != null) {
            m.put("model-data", String.valueOf(item.getModelData()));
        }
        if (item.getDurability() != null) {
            m.put("durability", String.valueOf(item.getDurability()));
        }
        if (item.getDamage() != null) {
            m.put("damage", String.valueOf(item.getDamage()));
        }
        if (!item.data().enchants().isEmpty()) {
            m.put("enchants", item.data().enchants().toString());
        }
        if (item instanceof NoEWeapon w) {
            m.put("weapon-type", w.getType().name());
            if (w.isRanged()) {
                m.put("ammo", w.getAmmo() == null ? "none" : w.getAmmo());
                m.put("magazine", String.valueOf(w.getMagazine()));
                m.put("reload-time", w.getReloadTime() + " ticks");
                m.put("fire-rate", w.getFireRate() + " ticks");
                m.put("range", String.valueOf(w.getRange()));
            }
        } else if (item instanceof NoEArmor a) {
            m.put("slot", String.valueOf(a.getSlot()));
            m.put("melee-protection", String.valueOf(a.getMeleeProtection()));
            m.put("firearm-protection", String.valueOf(a.getFirearmProtection()));
        } else if (item instanceof NoEBlock b) {
            m.put("hardness", String.valueOf(b.getHardness()));
            m.put("placed-material", b.getPlacedMaterial().name());
            m.put("drops", b.getDrops().stream().map(d -> d.ref() + "x" + d.min() + (d.max() != d.min() ? "-" + d.max() : ""))
                    .toList().toString());
        } else if (item instanceof NoEFood f && f.getFood() != null) {
            m.put("nutrition", String.valueOf(f.getFood().nutrition()));
            m.put("saturation", String.valueOf(f.getFood().saturation()));
        }
        if (!item.getEffects().isEmpty()) {
            m.put("effects", item.getEffects().stream()
                    .map(e -> e.type() + " " + (e.amplifier() + 1) + " (" + e.duration() + "t)").toList().toString());
        }
        m.put("file", item.getSource());
        return m;
    }

    // ------------------------------------------------------------------ reload / pack / menu

    private void reload(CommandSender sender) {
        plugin.messages().send(sender, "reload-start");
        plugin.reload().thenAccept(r -> {
            plugin.messages().send(sender, "reload-done",
                    "items", String.valueOf(r.items()), "weapons", String.valueOf(r.weapons()),
                    "armor", String.valueOf(r.armor()), "blocks", String.valueOf(r.blocks()),
                    "food", String.valueOf(r.food()), "errors", String.valueOf(r.errors()),
                    "warnings", String.valueOf(r.warnings()));
            if (r.errors() > 0) {
                plugin.messages().send(sender, "reload-errors");
            }
        });
    }

    private void pack(CommandSender sender, String[] args) {
        if (!plugin.settings().pack().enabled()) {
            plugin.messages().send(sender, "pack-disabled");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("send")) {
            if (args.length >= 3) {
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    plugin.messages().send(sender, "player-not-found", "player", args[2]);
                    return;
                }
                plugin.messages().send(sender, "pack-sent", "count", plugin.pack().send(target) ? "1" : "0");
            } else {
                plugin.messages().send(sender, "pack-sent", "count", String.valueOf(plugin.pack().sendToAll()));
            }
            return;
        }
        plugin.messages().send(sender, "pack-start");
        plugin.pack().regenerate().thenAccept(outcome -> {
            if (outcome.ok()) {
                plugin.messages().send(sender, "pack-done", "hash", outcome.result().sha1(),
                        "size", String.valueOf(outcome.result().size()));
            } else {
                plugin.messages().send(sender, "pack-failed", "reason", outcome.error());
            }
        });
    }

    private void menu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        String query = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "";
        plugin.menu().open(player, query, 0);
    }

    // ------------------------------------------------------------------ completion

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length <= 1) {
            List<String> subs = new ArrayList<>();
            subs.add("help");
            SUBCOMMANDS.forEach((name, perm) -> {
                if (allowed(sender, perm)) {
                    subs.add(name);
                }
            });
            subs.sort(String::compareTo);
            return CommandArgs.filter(subs, args.length == 0 ? "" : args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String permission = SUBCOMMANDS.get(sub);
        if (permission == null || !allowed(sender, permission)) {
            return List.of();
        }
        String current = args[args.length - 1];
        return switch (sub) {
            case "give" -> switch (args.length) {
                case 2 -> CommandArgs.filter(onlineNames(), current);
                case 3 -> CommandArgs.filter(plugin.factory().registry().ids(), current);
                case 4 -> CommandArgs.filter(List.of("1", "16", "32", "64"), current);
                default -> List.of();
            };
            case "info" -> args.length == 2 ? CommandArgs.filter(plugin.factory().registry().ids(), current) : List.of();
            case "pack" -> switch (args.length) {
                case 2 -> CommandArgs.filter(List.of("send"), current);
                case 3 -> args[1].equalsIgnoreCase("send") ? CommandArgs.filter(onlineNames(), current) : List.of();
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private static List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
