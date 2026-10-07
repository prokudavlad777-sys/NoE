package ua.noe.block;

import org.bukkit.Material;
import ua.noe.item.ItemData;
import ua.noe.item.NoEItem;

import java.util.List;

/** A custom block. The block item carries the definition; placed blocks are tracked in chunk data. */
public class NoEBlock extends NoEItem {

    private final double hardness;
    private final Material placedMaterial;
    private final boolean display;
    private final double displayScale;
    private final List<DropSpec> drops;
    private final boolean vanillaInteraction;
    private final List<String> interactCommands;

    public NoEBlock(ItemData data, double hardness, Material placedMaterial, boolean display, double displayScale,
                    List<DropSpec> drops, boolean vanillaInteraction, List<String> interactCommands) {
        super(data);
        this.hardness = hardness;
        this.placedMaterial = placedMaterial;
        this.display = display;
        this.displayScale = displayScale;
        this.drops = drops;
        this.vanillaInteraction = vanillaInteraction;
        this.interactCommands = interactCommands;
    }

    public double getHardness() {
        return hardness;
    }

    /** The vanilla block actually placed in the world. */
    public Material getPlacedMaterial() {
        return placedMaterial;
    }

    /** Whether an item display entity renders the custom model on top of the block. */
    public boolean usesDisplay() {
        return display;
    }

    public double getDisplayScale() {
        return displayScale;
    }

    public List<DropSpec> getDrops() {
        return drops;
    }

    /** If false, the vanilla right-click behaviour (e.g. opening a crafting table) is blocked. */
    public boolean allowsVanillaInteraction() {
        return vanillaInteraction;
    }

    /** Console commands run on right-click; {@code %player%} is replaced. */
    public List<String> getInteractCommands() {
        return interactCommands;
    }
}
