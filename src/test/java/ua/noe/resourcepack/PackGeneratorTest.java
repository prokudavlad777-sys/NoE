package ua.noe.resourcepack;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.noe.TestSupport;
import ua.noe.config.ConfigReport;
import ua.noe.config.NoESettings;
import ua.noe.item.ItemCategory;
import ua.noe.item.NoEItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackGeneratorTest {

    private static final NoESettings.PackSettings SETTINGS = new NoESettings.PackSettings(
            true, true, true, false, "", 84, 200, "Test pack", "", false, "0.0.0.0", 8765, "127.0.0.1");

    @TempDir
    Path dir;

    private static NoEItem item(String id, String material, int modelData) {
        ConfigReport r = new ConfigReport();
        NoEItem i = TestSupport.parse(ItemCategory.ITEM,
                "id: " + id + "\nmaterial: " + material + "\nmodel-data: " + modelData + "\n", r);
        assertNotNull(i, () -> r.issues().toString());
        return i;
    }

    @Test
    void rangeDispatchClosesEachRangeWithTheVanillaModel() {
        PackGenerator gen = new PackGenerator(m -> false);
        String json = gen.rangeDispatch(Material.DIAMOND_SWORD, List.of(item("a", "DIAMOND_SWORD", 1001), item("b", "DIAMOND_SWORD", 1005)));
        assertTrue(json.contains("\"threshold\": 1001"));
        assertTrue(json.contains("\"threshold\": 1002"));
        assertTrue(json.contains("\"threshold\": 1005"));
        assertTrue(json.contains("\"threshold\": 1006"));
        assertTrue(json.contains("minecraft:item/diamond_sword"));
        assertTrue(json.contains("noe:item/a"));
    }

    @Test
    void consecutiveModelDataDoesNotInsertAClosingEntryBetween() {
        PackGenerator gen = new PackGenerator(m -> false);
        String json = gen.rangeDispatch(Material.BONE, List.of(item("a", "BONE", 7), item("b", "BONE", 8)));
        assertTrue(json.contains("\"threshold\": 7"));
        assertTrue(json.contains("\"threshold\": 8"));
        assertTrue(json.contains("\"threshold\": 9"));
        assertEquals(3, json.split("\"threshold\"", -1).length - 1);
    }

    @Test
    void blockMaterialsFallBackToTheBlockModel() {
        PackGenerator gen = new PackGenerator(m -> true);
        assertTrue(gen.rangeDispatch(Material.CRAFTING_TABLE, List.of(item("w", "CRAFTING_TABLE", 4001)))
                .contains("minecraft:block/crafting_table"));
    }

    @Test
    void generatesZipWithModelsAndPackMeta() throws IOException {
        Path source = dir.resolve("resourcepack");
        Files.createDirectories(source.resolve("assets/noe/textures/item"));
        Files.write(source.resolve("assets/noe/textures/item/a.png"), new byte[]{1, 2, 3});

        List<String> warnings = new ArrayList<>();
        PackGenerator gen = new PackGenerator(m -> false);
        var result = gen.generate(List.of(item("a", "DIAMOND_SWORD", 1001), item("no_texture", "BONE", 1002)),
                source, dir.resolve("cache/pack.zip"), SETTINGS, warnings);

        Set<String> entries = new HashSet<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(result.file()))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                entries.add(e.getName());
            }
        }
        assertTrue(entries.contains("pack.mcmeta"));
        assertTrue(entries.contains("assets/noe/textures/item/a.png"));
        assertTrue(entries.contains("assets/noe/models/item/a.json"));
        assertTrue(entries.contains("assets/minecraft/items/diamond_sword.json"));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("no_texture")));
        assertEquals(40, result.sha1().length());
    }

    @Test
    void sameInputGivesTheSameHash() throws IOException {
        Path source = dir.resolve("resourcepack");
        Files.createDirectories(source.resolve("assets/noe/textures/item"));
        Files.write(source.resolve("assets/noe/textures/item/a.png"), new byte[]{9, 9});
        PackGenerator gen = new PackGenerator(m -> false);
        var items = List.of(item("a", "DIAMOND_SWORD", 1001));
        var first = gen.generate(items, source, dir.resolve("c1/p.zip"), SETTINGS, new ArrayList<>());
        var second = gen.generate(items, source, dir.resolve("c2/p.zip"), SETTINGS, new ArrayList<>());
        assertEquals(first.sha1(), second.sha1());
    }

    @Test
    void sha1MatchesKnownVector() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", PackGenerator.sha1("abc".getBytes()));
    }

    @Test
    void packMetaContainsFormatsAndEscapesDescription() {
        PackGenerator gen = new PackGenerator(m -> false);
        NoESettings.PackSettings s = new NoESettings.PackSettings(true, true, true, false, "", 10, 20,
                "He said \"hi\"", "", false, "0.0.0.0", 8765, "127.0.0.1");
        String meta = gen.packMeta(s);
        assertTrue(meta.contains("\"min_format\": 10"));
        assertTrue(meta.contains("\"max_format\": 20"));
        assertTrue(meta.contains("\\\"hi\\\""));
    }
}
