package ua.noe.config;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import ua.noe.TestSupport;
import ua.noe.armor.NoEArmor;
import ua.noe.block.DropSpec;
import ua.noe.block.NoEBlock;
import ua.noe.food.NoEFood;
import ua.noe.item.ItemCategory;
import ua.noe.item.NoEItem;
import ua.noe.weapon.NoEWeapon;
import ua.noe.weapon.WeaponType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemParserTest {

    @Test
    void parsesSpecExampleItem() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.ITEM, """
                id: tiger_sword
                material: DIAMOND_SWORD
                name: "&6Tiger Sword"
                lore:
                  - "&7line"
                model-data: 1001
                durability: 500
                damage: 12
                enchants:
                  sharpness: 2
                """, report);
        assertNotNull(item, () -> report.issues().toString());
        assertFalse(report.hasErrors());
        assertEquals("tiger_sword", item.getId());
        assertEquals(Material.DIAMOND_SWORD, item.getMaterial());
        assertEquals(1001, item.getModelData());
        assertEquals(500, item.getDurability());
        assertEquals(12.0, item.getDamage());
        assertEquals(2, item.data().enchants().get("sharpness"));
    }

    @Test
    void usesFallbackIdWhenIdIsMissing() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.ITEM, "material: STONE\n", report);
        assertNotNull(item);
        assertEquals("fallback", item.getId());
    }

    @Test
    void missingMaterialIsAReportedErrorNotAnException() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.ITEM, "id: broken\nname: x\n", report);
        assertNull(item);
        assertEquals(1, report.errorCount());
        ConfigIssue issue = report.issues().get(0);
        assertEquals("material", issue.parameter());
        assertEquals("broken", issue.id());
        assertEquals("test.yml", issue.file());
        assertFalse(issue.fix().isBlank());
    }

    @Test
    void unknownMaterialIsReported() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.ITEM, "id: a\nmaterial: DIAMOND_SWRD\n", report));
        assertEquals("material", report.issues().get(0).parameter());
    }

    @Test
    void invalidIdIsRejected() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.ITEM, "id: Bad Id\nmaterial: STONE\n", report));
        assertEquals("id", report.issues().get(0).parameter());
    }

    @Test
    void wrongNumberTypeAndRangeAreReported() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.ITEM, "id: a\nmaterial: STONE\ndamage: lots\ndurability: -5\n", report));
        assertEquals(2, report.errorCount());
    }

    @Test
    void parsesFirearm() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.WEAPON, """
                id: revolver
                type: FIREARM
                material: IRON_HORSE_ARMOR
                damage: 25
                ammo: pistol_ammo
                magazine: 6
                reload-time: 40
                model-data: 2001
                """, report);
        NoEWeapon w = assertInstanceOf(NoEWeapon.class, item, () -> report.issues().toString());
        assertEquals(WeaponType.FIREARM, w.getType());
        assertEquals("pistol_ammo", w.getAmmo());
        assertEquals(6, w.getMagazine());
        assertEquals(40, w.getReloadTime());
        assertEquals(25.0, w.getShotDamage());
        assertTrue(w.isRanged());
    }

    @Test
    void unknownWeaponTypeIsReported() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.WEAPON, "id: x\nmaterial: STONE\ntype: LASER\n", report));
        assertEquals("type", report.issues().get(0).parameter());
    }

    @Test
    void parsesArmor() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.ARMOR, """
                id: tiger_poncho
                type: ARMOR
                slot: CHEST
                material: LEATHER_CHESTPLATE
                melee-protection: 0.75
                firearm-protection: 0.45
                """, report);
        NoEArmor armor = assertInstanceOf(NoEArmor.class, item, () -> report.issues().toString());
        assertEquals(EquipmentSlot.CHEST, armor.getSlot());
        assertEquals(0.75, armor.getMeleeProtection());
        assertEquals(0.45, armor.getFirearmProtection());
    }

    @Test
    void armorProtectionOutOfRangeIsRejected() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.ARMOR, "id: a\nslot: HEAD\nmaterial: LEATHER_HELMET\nmelee-protection: 1.5\n", report));
        assertEquals("melee-protection", report.issues().get(0).parameter());
    }

    @Test
    void armorNeedsASlot() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.ARMOR, "id: a\nmaterial: LEATHER_HELMET\n", report));
        assertEquals("slot", report.issues().get(0).parameter());
    }

    @Test
    void parsesBlockWithDrops() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.BLOCK, """
                id: tropical_workbench
                material: CRAFTING_TABLE
                model-data: 4001
                hardness: 3.0
                drops:
                  - self
                  - tiger_fang:1-3
                  - STICK:2
                interact:
                  vanilla: false
                """, report);
        NoEBlock block = assertInstanceOf(NoEBlock.class, item, () -> report.issues().toString());
        assertEquals(3.0, block.getHardness());
        assertEquals(Material.CRAFTING_TABLE, block.getPlacedMaterial());
        assertEquals(3, block.getDrops().size());
        assertEquals(new DropSpec("self", 1, 1), block.getDrops().get(0));
        assertEquals(new DropSpec("tiger_fang", 1, 3), block.getDrops().get(1));
        assertEquals(new DropSpec("STICK", 2, 2), block.getDrops().get(2));
        assertFalse(block.allowsVanillaInteraction());
    }

    @Test
    void badDropAmountIsReported() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.BLOCK, "id: b\nmaterial: STONE\ndrops:\n  - self:abc\n", report));
        assertEquals("drops", report.issues().get(0).parameter());
    }

    @Test
    void parsesFoodWithEffects() {
        ConfigReport report = new ConfigReport();
        NoEItem item = TestSupport.parse(ItemCategory.FOOD, """
                id: tropical_fruit
                material: APPLE
                food:
                  nutrition: 6
                  saturation: 1.2
                effects:
                  regeneration:
                    duration: 100
                    amplifier: 0
                """, report);
        NoEFood food = assertInstanceOf(NoEFood.class, item, () -> report.issues().toString());
        assertEquals(6, food.getFood().nutrition());
        assertEquals(1.2f, food.getFood().saturation());
        assertEquals(1, food.getEffects().size());
        assertEquals("regeneration", food.getEffects().get(0).type());
        assertEquals(100, food.getEffects().get(0).duration());
    }

    @Test
    void foodWithoutFoodSectionIsReported() {
        ConfigReport report = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.FOOD, "id: f\nmaterial: APPLE\n", report));
        assertEquals("food", report.issues().get(0).parameter());
    }

    @Test
    void attributesAreValidated() {
        ConfigReport report = new ConfigReport();
        NoEItem ok = TestSupport.parse(ItemCategory.ITEM, """
                id: a
                material: STONE
                attributes:
                  attack_speed:
                    amount: -2.4
                    operation: ADD_NUMBER
                    slot: mainhand
                """, report);
        assertNotNull(ok, () -> report.issues().toString());
        assertEquals(1, ok.data().attributes().size());

        ConfigReport bad = new ConfigReport();
        assertNull(TestSupport.parse(ItemCategory.ITEM, """
                id: b
                material: STONE
                attributes:
                  attack_speed:
                    amount: 1
                    operation: NOPE
                """, bad));
        assertEquals("attributes.attack_speed.operation", bad.issues().get(0).parameter());
    }
}
