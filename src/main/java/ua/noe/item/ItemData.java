package ua.noe.item;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/** Immutable, server-independent data shared by every NoE definition. */
public record ItemData(
        String id,
        ItemCategory category,
        String source,
        Material material,
        String name,
        List<String> lore,
        Integer modelData,
        String itemModel,
        Integer durability,
        Double damage,
        boolean unbreakable,
        Map<String, Integer> enchants,
        List<String> flags,
        List<AttributeSpec> attributes,
        FoodSpec food,
        List<EffectSpec> effects,
        Float consumeSeconds,
        EquipSpec equipment
) {

    public record AttributeSpec(String attribute, double amount, String operation, String slot) {
    }

    public record EffectSpec(String type, int duration, int amplifier) {
    }

    public record FoodSpec(int nutrition, float saturation, boolean canAlwaysEat) {
    }

    public record EquipSpec(String slot, String model) {
    }
}
