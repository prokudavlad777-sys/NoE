package ua.noe.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import ua.noe.armor.NoEArmor;
import ua.noe.block.DropSpec;
import ua.noe.block.NoEBlock;
import ua.noe.food.NoEFood;
import ua.noe.item.ItemCategory;
import ua.noe.item.ItemData;
import ua.noe.item.NoEItem;
import ua.noe.util.IdValidator;
import ua.noe.weapon.NoEWeapon;
import ua.noe.weapon.WeaponType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Turns one YAML section into a NoE definition. Pure parsing and validation: it needs no running
 * server, which keeps it unit-testable. Every problem is recorded in the {@link ConfigReport}.
 */
public final class ItemParser {

    private static final Set<String> ATTRIBUTE_SLOTS = Set.of(
            "any", "mainhand", "offhand", "hand", "head", "chest", "legs", "feet", "armor", "body");

    private final ConfigReport report;
    private final Predicate<Material> isItem;
    private final Predicate<Material> isBlock;

    private String file;
    private String id;

    public ItemParser(ConfigReport report, Predicate<Material> isItem, Predicate<Material> isBlock) {
        this.report = report;
        this.isItem = isItem;
        this.isBlock = isBlock;
    }

    /**
     * @return the parsed definition, or {@code null} if the section is invalid (errors are in the report)
     */
    public NoEItem parse(ItemCategory category, String file, String fallbackId, ConfigurationSection s) {
        this.file = file;
        this.id = s.getString("id", fallbackId);
        int errorsBefore = report.errorCount();

        if (!IdValidator.isValid(id)) {
            report.error(file, id, "id", "Invalid id '" + id + "'", "Use " + IdValidator.requirementsText());
            return null;
        }

        Material material = readMaterial(s, category);
        String name = s.getString("name");
        List<String> lore = s.getStringList("lore");
        Integer modelData = readInt(s, "model-data", null, 0, Integer.MAX_VALUE);
        String itemModel = readNamespacedKey(s, "item-model");
        Integer durability = readInt(s, "durability", null, 1, Short.MAX_VALUE);
        Double damage = readDouble(s, "damage", null, 0.0, 1024.0);
        boolean unbreakable = readBoolean(s, "unbreakable", false);
        Map<String, Integer> enchants = readEnchants(s);
        List<String> flags = readFlags(s);
        List<ItemData.AttributeSpec> attributes = readAttributes(s);
        ItemData.FoodSpec food = readFood(s, category == ItemCategory.FOOD);
        List<ItemData.EffectSpec> effects = readEffects(s);
        Float consumeSeconds = readFloat(s, "consume-seconds");
        ItemData.EquipSpec equipment = readEquipment(s);

        if (material == null || report.errorCount() > errorsBefore) {
            return null;
        }

        ItemData data = new ItemData(id, category, file, material, name, List.copyOf(lore), modelData, itemModel,
                durability, damage, unbreakable, Map.copyOf(enchants), List.copyOf(flags), List.copyOf(attributes),
                food, List.copyOf(effects), consumeSeconds, equipment);

        NoEItem result = switch (category) {
            case ITEM -> new NoEItem(data);
            case FOOD -> new NoEFood(data);
            case WEAPON -> parseWeapon(s, data);
            case ARMOR -> parseArmor(s, data);
            case BLOCK -> parseBlock(s, data);
        };
        return report.errorCount() > errorsBefore ? null : result;
    }

    // ---------------------------------------------------------------- categories

    private NoEWeapon parseWeapon(ConfigurationSection s, ItemData data) {
        WeaponType type = WeaponType.FIREARM;
        String rawType = s.getString("type");
        if (rawType != null) {
            try {
                type = WeaponType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                report.error(file, id, "type", "Unknown weapon type '" + rawType + "'",
                        "Use one of " + Arrays.toString(WeaponType.values()));
            }
        }
        String ammo = s.getString("ammo");
        if (ammo != null && !IdValidator.isValid(ammo)) {
            report.error(file, id, "ammo", "Invalid ammo id '" + ammo + "'",
                    "Use the id of a NoE item, " + IdValidator.requirementsText());
            ammo = null;
        }
        int magazine = readInt(s, "magazine", 6, 1, 10_000);
        int reload = readInt(s, "reload-time", 40, 0, 12_000);
        int fireRate = readInt(s, "fire-rate", 10, 0, 1_200);
        double range = readDouble(s, "range", 60.0, 1.0, 512.0);
        int pellets = readInt(s, "pellets", 1, 1, 64);
        double spread = readDouble(s, "spread", 0.0, 0.0, 45.0);
        double speed = readDouble(s, "projectile-speed", 3.0, 0.1, 10.0);
        String shoot = s.getString("sounds.shoot", "minecraft:entity.firework_rocket.blast");
        String reloadSound = s.getString("sounds.reload", "minecraft:item.crossbow.loading_end");
        String empty = s.getString("sounds.empty", "minecraft:block.dispenser.fail");
        if (type.isRanged() && data.damage() == null) {
            report.warn(file, id, "damage", "Ranged weapon has no damage, defaulting to 1.0",
                    "Add 'damage: <number>'");
        }
        return new NoEWeapon(data, type, ammo, magazine, reload, fireRate, range, pellets, spread, speed,
                shoot, reloadSound, empty);
    }

    private NoEArmor parseArmor(ConfigurationSection s, ItemData data) {
        String rawType = s.getString("type", "ARMOR");
        if (!rawType.equalsIgnoreCase("ARMOR")) {
            report.warn(file, id, "type", "Armor files should use 'type: ARMOR' (found '" + rawType + "')",
                    "Set 'type: ARMOR'");
        }
        EquipmentSlot slot = null;
        String rawSlot = s.getString("slot");
        if (rawSlot == null) {
            report.error(file, id, "slot", "Missing armor slot", "Add 'slot: HEAD | CHEST | LEGS | FEET'");
        } else {
            slot = switch (rawSlot.trim().toUpperCase(Locale.ROOT)) {
                case "HEAD", "HELMET" -> EquipmentSlot.HEAD;
                case "CHEST", "CHESTPLATE" -> EquipmentSlot.CHEST;
                case "LEGS", "LEGGINGS" -> EquipmentSlot.LEGS;
                case "FEET", "BOOTS" -> EquipmentSlot.FEET;
                default -> null;
            };
            if (slot == null) {
                report.error(file, id, "slot", "Unknown armor slot '" + rawSlot + "'",
                        "Use HEAD, CHEST, LEGS or FEET");
            }
        }
        double melee = readDouble(s, "melee-protection", 0.0, 0.0, 1.0);
        double firearm = readDouble(s, "firearm-protection", 0.0, 0.0, 1.0);
        return new NoEArmor(data, slot, melee, firearm);
    }

    private NoEBlock parseBlock(ConfigurationSection s, ItemData data) {
        double hardness = readDouble(s, "hardness", 1.0, 0.0, 1_000_000.0);
        Material placed = data.material();
        String rawPlaced = s.getString("placed-material");
        if (rawPlaced != null) {
            Material m = Material.matchMaterial(rawPlaced);
            if (m == null || !isBlock.test(m)) {
                report.error(file, id, "placed-material", "'" + rawPlaced + "' is not a placeable block",
                        "Use a block material such as STONE or BARRIER");
            } else {
                placed = m;
            }
        }
        boolean display = readBoolean(s, "display", false);
        double scale = readDouble(s, "display-scale", 1.0, 0.01, 64.0);
        List<DropSpec> drops = new ArrayList<>();
        List<String> rawDrops = s.contains("drops") ? s.getStringList("drops") : List.of("self");
        for (String raw : rawDrops) {
            DropSpec drop = parseDrop(raw);
            if (drop != null) {
                drops.add(drop);
            }
        }
        boolean vanilla = readBoolean(s, "interact.vanilla", true);
        List<String> commands = s.getStringList("interact.commands");
        return new NoEBlock(data, hardness, placed, display, scale, List.copyOf(drops), vanilla, List.copyOf(commands));
    }

    DropSpec parseDrop(String raw) {
        if (raw == null || raw.isBlank()) {
            report.error(file, id, "drops", "Empty drop entry", "Use 'self', '<item_id>:<amount>' or '<MATERIAL>:<min>-<max>'");
            return null;
        }
        String ref = raw.trim();
        int min = 1;
        int max = 1;
        int colon = ref.indexOf(':');
        if (colon >= 0) {
            String amount = ref.substring(colon + 1).trim();
            ref = ref.substring(0, colon).trim();
            try {
                int dash = amount.indexOf('-');
                if (dash > 0) {
                    min = Integer.parseInt(amount.substring(0, dash).trim());
                    max = Integer.parseInt(amount.substring(dash + 1).trim());
                } else {
                    min = Integer.parseInt(amount);
                    max = min;
                }
            } catch (NumberFormatException ex) {
                report.error(file, id, "drops", "Invalid drop amount in '" + raw + "'", "Use a number or a range like 1-3");
                return null;
            }
            if (min < 0 || max < min || max > 64) {
                report.error(file, id, "drops", "Drop amount out of range in '" + raw + "'", "Use 0-64 with min <= max");
                return null;
            }
        }
        if (ref.isEmpty()) {
            report.error(file, id, "drops", "Drop '" + raw + "' has no item", "Write 'self' or an item id");
            return null;
        }
        return new DropSpec(ref.equalsIgnoreCase(DropSpec.SELF) ? DropSpec.SELF : ref, min, max);
    }

    // ---------------------------------------------------------------- field readers

    private Material readMaterial(ConfigurationSection s, ItemCategory category) {
        String raw = s.getString("material");
        if (raw == null) {
            report.error(file, id, "material", "Missing material", "Add 'material: DIAMOND_SWORD' (any Bukkit item material)");
            return null;
        }
        Material m = Material.matchMaterial(raw);
        if (m == null || !isItem.test(m)) {
            report.error(file, id, "material", "'" + raw + "' is not a valid item material",
                    "Use a Bukkit Material name, e.g. DIAMOND_SWORD");
            return null;
        }
        if (category == ItemCategory.BLOCK && !isBlock.test(m)) {
            report.error(file, id, "material", "'" + raw + "' cannot be placed as a block",
                    "Use a block material such as CRAFTING_TABLE and set 'placed-material' if needed");
            return null;
        }
        return m;
    }

    private String readNamespacedKey(ConfigurationSection s, String path) {
        String raw = s.getString(path);
        if (raw == null) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
        if (key == null) {
            report.error(file, id, path, "Invalid namespaced key '" + raw + "'", "Use the form 'namespace:path', e.g. noe:tiger_sword");
            return null;
        }
        return key.toString();
    }

    private Map<String, Integer> readEnchants(ConfigurationSection s) {
        Map<String, Integer> out = new LinkedHashMap<>();
        ConfigurationSection sec = s.getConfigurationSection("enchants");
        if (sec == null) {
            if (s.contains("enchants")) {
                report.error(file, id, "enchants", "Must be a map of enchantment to level", "Example: enchants:\n  sharpness: 2");
            }
            return out;
        }
        for (String key : sec.getKeys(false)) {
            Integer level = readInt(sec, key, 1, 1, 255, "enchants." + key);
            if (level != null) {
                out.put(key.toLowerCase(Locale.ROOT), level);
            }
        }
        return out;
    }

    private List<String> readFlags(ConfigurationSection s) {
        List<String> out = new ArrayList<>();
        for (String raw : s.getStringList("flags")) {
            try {
                out.add(ItemFlag.valueOf(raw.trim().toUpperCase(Locale.ROOT)).name());
            } catch (IllegalArgumentException ex) {
                report.warn(file, id, "flags", "Unknown item flag '" + raw + "' ignored", "Use a Bukkit ItemFlag name, e.g. HIDE_ATTRIBUTES");
            }
        }
        return out;
    }

    private List<ItemData.AttributeSpec> readAttributes(ConfigurationSection s) {
        List<ItemData.AttributeSpec> out = new ArrayList<>();
        ConfigurationSection sec = s.getConfigurationSection("attributes");
        if (sec == null) {
            return out;
        }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection a = sec.getConfigurationSection(key);
            String param = "attributes." + key;
            if (a == null) {
                report.error(file, id, param, "Must be a section with 'amount'", "Example:\n  " + key + ":\n    amount: 2.0");
                continue;
            }
            if (!a.contains("amount")) {
                report.error(file, id, param + ".amount", "Missing amount", "Add 'amount: <number>'");
                continue;
            }
            double amount = readDouble(a, "amount", 0.0, -1024.0, 1024.0, param + ".amount");
            String op = a.getString("operation", "ADD_NUMBER").trim().toUpperCase(Locale.ROOT);
            try {
                AttributeModifier.Operation.valueOf(op);
            } catch (IllegalArgumentException ex) {
                report.error(file, id, param + ".operation", "Unknown operation '" + op + "'",
                        "Use ADD_NUMBER, ADD_SCALAR or MULTIPLY_SCALAR_1");
                continue;
            }
            String slot = a.getString("slot", "any").trim().toLowerCase(Locale.ROOT);
            if (!ATTRIBUTE_SLOTS.contains(slot)) {
                report.error(file, id, param + ".slot", "Unknown slot '" + slot + "'", "Use one of " + ATTRIBUTE_SLOTS);
                continue;
            }
            out.add(new ItemData.AttributeSpec(key, amount, op, slot));
        }
        return out;
    }

    private ItemData.FoodSpec readFood(ConfigurationSection s, boolean required) {
        ConfigurationSection f = s.getConfigurationSection("food");
        if (f == null) {
            if (required) {
                report.error(file, id, "food", "Food items need a 'food' section",
                        "Add:\nfood:\n  nutrition: 6\n  saturation: 1.2");
            }
            return null;
        }
        int nutrition = readInt(f, "nutrition", 4, 0, 100, "food.nutrition");
        float saturation = (float) readDouble(f, "saturation", 1.0, 0.0, 100.0, "food.saturation");
        boolean always = readBoolean(f, "can-always-eat", false);
        return new ItemData.FoodSpec(nutrition, saturation, always);
    }

    private List<ItemData.EffectSpec> readEffects(ConfigurationSection s) {
        List<ItemData.EffectSpec> out = new ArrayList<>();
        ConfigurationSection sec = s.getConfigurationSection("effects");
        if (sec == null) {
            return out;
        }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection e = sec.getConfigurationSection(key);
            String param = "effects." + key;
            if (e == null) {
                report.error(file, id, param, "Must be a section with 'duration' and 'amplifier'",
                        "Example:\n  " + key + ":\n    duration: 100\n    amplifier: 0");
                continue;
            }
            int duration = readInt(e, "duration", 100, 1, 1_000_000, param + ".duration");
            int amplifier = readInt(e, "amplifier", 0, 0, 255, param + ".amplifier");
            out.add(new ItemData.EffectSpec(key.toLowerCase(Locale.ROOT), duration, amplifier));
        }
        return out;
    }

    private ItemData.EquipSpec readEquipment(ConfigurationSection s) {
        ConfigurationSection e = s.getConfigurationSection("equipment");
        if (e == null) {
            return null;
        }
        String rawSlot = e.getString("slot");
        if (rawSlot == null) {
            report.error(file, id, "equipment.slot", "Missing slot", "Add 'slot: HEAD | CHEST | LEGS | FEET'");
            return null;
        }
        String slot = rawSlot.trim().toUpperCase(Locale.ROOT);
        try {
            EquipmentSlot.valueOf(slot);
        } catch (IllegalArgumentException ex) {
            report.error(file, id, "equipment.slot", "Unknown slot '" + rawSlot + "'", "Use HEAD, CHEST, LEGS or FEET");
            return null;
        }
        String model = readNamespacedKey(e, "model");
        return new ItemData.EquipSpec(slot, model);
    }

    // ---------------------------------------------------------------- primitives

    private Integer readInt(ConfigurationSection s, String path, Integer def, int min, int max) {
        return readInt(s, path, def, min, max, path);
    }

    private Integer readInt(ConfigurationSection s, String path, Integer def, int min, int max, String param) {
        if (!s.contains(path)) {
            return def;
        }
        Object o = s.get(path);
        Long value = null;
        if (o instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            value = n.longValue();
        } else if (o instanceof String str) {
            try {
                value = Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        if (value == null) {
            report.error(file, id, param, "Expected a whole number but found '" + o + "'", "Use an integer such as 10");
            return def;
        }
        if (value < min || value > max) {
            report.error(file, id, param, "Value " + value + " is out of range " + min + ".." + max,
                    "Use a value between " + min + " and " + max);
            return def;
        }
        return value.intValue();
    }

    private double readDouble(ConfigurationSection s, String path, double def, double min, double max) {
        Double d = readDouble(s, path, (Double) def, min, max, path);
        return d == null ? def : d;
    }

    private double readDouble(ConfigurationSection s, String path, double def, double min, double max, String param) {
        Double d = readDouble(s, path, (Double) def, min, max, param);
        return d == null ? def : d;
    }

    private Double readDouble(ConfigurationSection s, String path, Double def, double min, double max) {
        return readDouble(s, path, def, min, max, path);
    }

    private Double readDouble(ConfigurationSection s, String path, Double def, double min, double max, String param) {
        if (!s.contains(path)) {
            return def;
        }
        Object o = s.get(path);
        Double value = null;
        if (o instanceof Number n) {
            value = n.doubleValue();
        } else if (o instanceof String str) {
            try {
                value = Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        if (value == null || value.isNaN() || value.isInfinite()) {
            report.error(file, id, param, "Expected a number but found '" + o + "'", "Use a number such as 2.5");
            return def;
        }
        if (value < min || value > max) {
            report.error(file, id, param, "Value " + value + " is out of range " + min + ".." + max,
                    "Use a value between " + min + " and " + max);
            return def;
        }
        return value;
    }

    private Float readFloat(ConfigurationSection s, String path) {
        Double d = readDouble(s, path, (Double) null, 0.0, 3600.0, path);
        return d == null ? null : d.floatValue();
    }

    private boolean readBoolean(ConfigurationSection s, String path, boolean def) {
        if (!s.contains(path)) {
            return def;
        }
        Object o = s.get(path);
        if (o instanceof Boolean b) {
            return b;
        }
        report.error(file, id, path, "Expected true or false but found '" + o + "'", "Use 'true' or 'false'");
        return def;
    }
}
