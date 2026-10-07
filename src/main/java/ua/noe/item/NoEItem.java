package ua.noe.item;

import org.bukkit.Material;
import ua.noe.api.NoEDefinition;

import java.util.List;

/** A custom NoE item definition. Subclasses add weapon, armor, block and food data. */
public class NoEItem implements NoEDefinition {

    private final ItemData data;

    public NoEItem(ItemData data) {
        this.data = data;
    }

    public ItemData data() {
        return data;
    }

    @Override
    public String getId() {
        return data.id();
    }

    @Override
    public ItemCategory getCategory() {
        return data.category();
    }

    @Override
    public Material getMaterial() {
        return data.material();
    }

    public String getName() {
        return data.name();
    }

    public List<String> getLore() {
        return data.lore();
    }

    public Integer getModelData() {
        return data.modelData();
    }

    public Double getDamage() {
        return data.damage();
    }

    public Integer getDurability() {
        return data.durability();
    }

    public String getSource() {
        return data.source();
    }

    public List<ItemData.EffectSpec> getEffects() {
        return data.effects();
    }
}
