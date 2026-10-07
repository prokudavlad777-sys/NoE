package ua.noe.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;
import ua.noe.armor.NoEArmor;
import ua.noe.config.ConfigReport;
import ua.noe.util.Keys;
import ua.noe.util.Registries;
import ua.noe.util.Text;
import ua.noe.weapon.NoEWeapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Fluent builder for item stacks. Not tied to any item type: every NoE category uses the same builder,
 * and other plugins can use it directly.
 */
public final class ItemBuilder {

    private final Material material;
    private int amount = 1;
    private final List<Consumer<ItemMeta>> metaOps = new ArrayList<>();
    private final List<Consumer<ItemStack>> stackOps = new ArrayList<>();

    private ItemBuilder(Material material) {
        this.material = material;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    /** Builds the base stack for a definition, including its PDC id. Unresolvable parts are reported as warnings. */
    public static ItemBuilder fromDefinition(NoEItem def, Keys keys, ConfigReport report) {
        ItemData d = def.data();
        ItemBuilder b = of(d.material());
        if (d.name() != null) {
            b.name(d.name());
        }
        if (!d.lore().isEmpty()) {
            b.lore(d.lore());
        }
        if (d.modelData() != null) {
            b.modelData(d.modelData());
        }
        if (d.itemModel() != null) {
            b.itemModel(d.itemModel());
        }
        if (d.durability() != null) {
            b.durability(d.durability());
        }
        b.unbreakable(d.unbreakable());

        d.enchants().forEach((name, level) -> {
            Enchantment e = Registries.enchantment(name);
            if (e == null) {
                warn(report, d, "enchants." + name, "Unknown enchantment '" + name + "'",
                        "Use a vanilla enchantment key such as sharpness");
            } else {
                b.enchant(e, level);
            }
        });

        for (ItemFlag flag : d.flags().stream().map(ItemFlag::valueOf).toList()) {
            b.flags(flag);
        }

        boolean hasAttackSpeed = false;
        for (ItemData.AttributeSpec spec : d.attributes()) {
            Attribute attribute = Registries.attribute(spec.attribute());
            EquipmentSlotGroup group = EquipmentSlotGroup.getByName(spec.slot());
            if (attribute == null) {
                warn(report, d, "attributes." + spec.attribute(), "Unknown attribute '" + spec.attribute() + "'",
                        "Use a vanilla attribute such as attack_speed or movement_speed");
                continue;
            }
            if (group == null) {
                warn(report, d, "attributes." + spec.attribute() + ".slot", "Unknown slot '" + spec.slot() + "'",
                        "Use mainhand, offhand, head, chest, legs, feet, armor or any");
                continue;
            }
            if (spec.attribute().toLowerCase(Locale.ROOT).endsWith("attack_speed")) {
                hasAttackSpeed = true;
            }
            b.attribute(attribute, new AttributeModifier(keys.modifier(d.id(), spec.attribute().replaceAll("[^a-z0-9._-]", "_")),
                    spec.amount(), AttributeModifier.Operation.valueOf(spec.operation()), group));
        }

        boolean meleeDamage = d.damage() != null
                && (def.getCategory() == ItemCategory.ITEM
                || (def instanceof NoEWeapon w && !w.isRanged()));
        if (meleeDamage) {
            Attribute attackDamage = Registries.attribute("attack_damage");
            if (attackDamage != null) {
                // The player's bare hands already deal 1 damage, the modifier adds the rest.
                b.attribute(attackDamage, new AttributeModifier(keys.modifier(d.id(), "damage"),
                        d.damage() - 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            }
            Attribute attackSpeed = Registries.attribute("attack_speed");
            if (!hasAttackSpeed && attackSpeed != null) {
                b.attribute(attackSpeed, new AttributeModifier(keys.modifier(d.id(), "attack_speed"),
                        -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            }
        }

        if (d.food() != null) {
            b.food(d.food().nutrition(), d.food().saturation(), d.food().canAlwaysEat());
            b.consumable(d.consumeSeconds() == null ? 1.6f : d.consumeSeconds());
        } else if (d.consumeSeconds() != null) {
            b.consumable(d.consumeSeconds());
        }
        if (d.equipment() != null) {
            b.equippable(EquipmentSlot.valueOf(d.equipment().slot()), d.equipment().model());
        } else if (def instanceof NoEArmor armor && armor.getSlot() != null) {
            b.equippable(armor.getSlot(), null);
        }

        if (def instanceof NoEWeapon w && w.isRanged()) {
            b.pdc(keys.ammo, PersistentDataType.INTEGER, w.getMagazine());
        }
        b.pdc(keys.itemId, PersistentDataType.STRING, d.id());
        return b;
    }

    private static void warn(ConfigReport report, ItemData d, String param, String reason, String fix) {
        if (report != null) {
            report.warn(d.source(), d.id(), param, reason, fix);
        }
    }

    // ------------------------------------------------------------------ fluent API

    public ItemBuilder amount(int amount) {
        this.amount = Math.max(1, amount);
        return this;
    }

    public ItemBuilder meta(Consumer<ItemMeta> op) {
        metaOps.add(op);
        return this;
    }

    public ItemBuilder name(String legacyText) {
        return meta(m -> m.displayName(Text.item(legacyText)));
    }

    public ItemBuilder lore(List<String> legacyLines) {
        return meta(m -> m.lore(Text.items(legacyLines)));
    }

    /** Custom model data (first float of the custom_model_data component). */
    public ItemBuilder modelData(int value) {
        return meta(m -> {
            CustomModelDataComponent c = m.getCustomModelDataComponent();
            c.setFloats(List.of((float) value));
            m.setCustomModelDataComponent(c);
        });
    }

    /** The {@code item_model} component, e.g. {@code noe:tiger_sword}. */
    public ItemBuilder itemModel(String namespacedKey) {
        NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        return key == null ? this : meta(m -> m.setItemModel(key));
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        return meta(m -> m.addEnchant(enchantment, level, true));
    }

    public ItemBuilder attribute(Attribute attribute, AttributeModifier modifier) {
        return meta(m -> m.addAttributeModifier(attribute, modifier));
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        return unbreakable ? meta(m -> m.setUnbreakable(true)) : this;
    }

    /** Sets the maximum durability. */
    public ItemBuilder durability(int maxDamage) {
        return meta(m -> {
            if (m instanceof Damageable damageable) {
                damageable.setMaxDamage(maxDamage);
            }
        });
    }

    public ItemBuilder flags(ItemFlag... flags) {
        return meta(m -> m.addItemFlags(flags));
    }

    public <P, C> ItemBuilder pdc(NamespacedKey key, PersistentDataType<P, C> type, C value) {
        return meta(m -> m.getPersistentDataContainer().set(key, type, value));
    }

    /** Stores free-form custom flags under {@code noe:flags}. */
    public ItemBuilder customFlags(NamespacedKey key, Collection<String> flags) {
        if (flags.isEmpty()) {
            return this;
        }
        return pdc(key, PersistentDataType.STRING, String.join(",", flags));
    }

    public ItemBuilder food(int nutrition, float saturation, boolean canAlwaysEat) {
        return meta(m -> {
            FoodComponent f = m.getFoodComponent();
            f.setNutrition(nutrition);
            f.setSaturation(saturation);
            f.setCanAlwaysEat(canAlwaysEat);
            m.setFood(f);
        });
    }

    /** Makes the item consumable with the given eating time. */
    public ItemBuilder consumable(float consumeSeconds) {
        stackOps.add(stack -> stack.setData(DataComponentTypes.CONSUMABLE,
                Consumable.consumable().consumeSeconds(consumeSeconds).build()));
        return this;
    }

    /** Makes the item wearable in a slot, optionally with a custom equipment model asset. */
    public ItemBuilder equippable(EquipmentSlot slot, String modelKey) {
        NamespacedKey model = modelKey == null ? null : NamespacedKey.fromString(modelKey);
        return meta(m -> {
            EquippableComponent e = m.getEquippable();
            e.setSlot(slot);
            if (model != null) {
                e.setModel(model);
            }
            m.setEquippable(e);
        });
    }

    public ItemStack build() {
        ItemStack stack = new ItemStack(material, amount);
        stack.editMeta(meta -> {
            for (Consumer<ItemMeta> op : metaOps) {
                op.accept(meta);
            }
        });
        for (Consumer<ItemStack> op : stackOps) {
            op.accept(stack);
        }
        return stack;
    }
}
