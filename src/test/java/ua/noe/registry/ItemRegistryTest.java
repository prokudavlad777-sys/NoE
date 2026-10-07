package ua.noe.registry;

import org.junit.jupiter.api.Test;
import ua.noe.TestSupport;
import ua.noe.config.ConfigReport;
import ua.noe.item.ItemCategory;
import ua.noe.item.NoEItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRegistryTest {

    private static NoEItem item(ItemCategory cat, String yaml) {
        ConfigReport r = new ConfigReport();
        NoEItem i = TestSupport.parse(cat, yaml, r);
        assertNotNull(i, () -> r.issues().toString());
        return i;
    }

    @Test
    void storesAndLooksUpByCategory() {
        ConfigReport report = new ConfigReport();
        ItemRegistry registry = ItemRegistry.builder(report)
                .add(item(ItemCategory.ITEM, "id: b_item\nmaterial: STONE\n"))
                .add(item(ItemCategory.WEAPON, "id: gun\nmaterial: IRON_HORSE_ARMOR\ndamage: 5\nammo: b_item\n"))
                .add(item(ItemCategory.ARMOR, "id: hat\nslot: HEAD\nmaterial: LEATHER_HELMET\n"))
                .build();
        assertEquals(3, registry.size());
        assertEquals(List.of("b_item", "gun", "hat"), registry.ids());
        assertTrue(registry.weapon("gun").isPresent());
        assertTrue(registry.weapon("hat").isEmpty());
        assertTrue(registry.armor("hat").isPresent());
        assertNull(registry.get("missing"));
        assertNull(registry.get(null));
        assertEquals(1, registry.count(ItemCategory.WEAPON));
        assertEquals(0, registry.count(ItemCategory.BLOCK));
        assertTrue(report.issues().isEmpty());
    }

    @Test
    void duplicateIdsAreRejected() {
        ConfigReport report = new ConfigReport();
        ItemRegistry registry = ItemRegistry.builder(report)
                .add(item(ItemCategory.ITEM, "id: x\nmaterial: STONE\n"))
                .add(item(ItemCategory.ITEM, "id: x\nmaterial: DIRT\n"))
                .build();
        assertEquals(1, registry.size());
        assertTrue(report.hasErrors());
    }

    @Test
    void missingAmmoItemProducesAWarning() {
        ConfigReport report = new ConfigReport();
        ItemRegistry.builder(report)
                .add(item(ItemCategory.WEAPON, "id: gun\nmaterial: IRON_HORSE_ARMOR\ndamage: 5\nammo: ghost_ammo\n"))
                .build();
        assertFalse(report.hasErrors());
        assertEquals(1, report.warningCount());
        assertEquals("ammo", report.issues().get(0).parameter());
    }

    @Test
    void searchMatchesIdAndName() {
        ItemRegistry registry = ItemRegistry.builder(new ConfigReport())
                .add(item(ItemCategory.ITEM, "id: tiger_sword\nmaterial: DIAMOND_SWORD\nname: \"&6Fierce Blade\"\n"))
                .add(item(ItemCategory.ITEM, "id: apple_pie\nmaterial: STONE\n"))
                .build();
        java.util.function.Function<NoEItem, String> plain = i -> i.getName() == null ? null : i.getName().replace("&6", "");
        assertEquals(List.of("tiger_sword"), registry.search("tiger", plain));
        assertEquals(List.of("tiger_sword"), registry.search("FIERCE", plain));
        assertEquals(2, registry.search("", plain).size());
        assertTrue(registry.search("zzz", plain).isEmpty());
    }

    @Test
    void emptyRegistryIsSafe() {
        assertEquals(0, ItemRegistry.EMPTY.size());
        assertTrue(ItemRegistry.EMPTY.ids().isEmpty());
        assertEquals(0, ItemRegistry.EMPTY.count(ItemCategory.ITEM));
    }
}
