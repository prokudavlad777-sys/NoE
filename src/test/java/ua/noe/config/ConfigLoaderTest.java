package ua.noe.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.noe.item.ItemCategory;
import ua.noe.registry.ItemRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @TempDir
    Path dir;

    /** A layout whose item folders already exist, so the bundled examples are not copied in. */
    private DirectoryLayout emptyLayout() throws IOException {
        Files.createDirectories(dir.resolve("items"));
        DirectoryLayout layout = new DirectoryLayout(dir);
        layout.install(Logger.getAnonymousLogger());
        return layout;
    }

    private ItemRegistry load(ConfigReport report) {
        DirectoryLayout layout = new DirectoryLayout(dir);
        layout.install(Logger.getAnonymousLogger());
        return new ConfigLoader(layout, m -> true, m -> true).load(report);
    }

    @Test
    void createsTheDirectoryStructure() {
        DirectoryLayout layout = new DirectoryLayout(dir);
        layout.install(Logger.getAnonymousLogger());
        for (ItemCategory c : ItemCategory.values()) {
            assertTrue(Files.isDirectory(layout.folder(c)), c.folder());
        }
        assertTrue(Files.isDirectory(layout.resourcePackDir()));
        assertTrue(Files.isDirectory(layout.cacheDir()));
    }

    @Test
    void bundledExamplesAllLoadWithoutErrors() {
        ConfigReport report = new ConfigReport();
        ItemRegistry registry = load(report);
        assertTrue(report.issues().isEmpty(), () -> report.issues().toString());
        assertNotNull(registry.get("tiger_sword"));
        assertNotNull(registry.get("revolver"));
        assertNotNull(registry.get("tiger_poncho"));
        assertNotNull(registry.get("tropical_workbench"));
        assertNotNull(registry.get("tropical_fruit"));
        assertEquals(3, registry.count(ItemCategory.ITEM));
        assertEquals(1, registry.count(ItemCategory.WEAPON));
        assertEquals(2, registry.count(ItemCategory.ARMOR));
    }

    @Test
    void brokenFilesNeverThrowAndGoodFilesStillLoad() throws IOException {
        DirectoryLayout layout = emptyLayout();
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("good.yml"), "id: good\nmaterial: STONE\n");
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("syntax.yml"), "id: [unclosed\n  material: : :\n");
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("nomat.yml"), "id: nomat\n");
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("empty.yml"), "");

        ConfigReport report = new ConfigReport();
        ItemRegistry registry = new ConfigLoader(layout, m -> true, m -> true).load(report);

        assertNotNull(registry.get("good"));
        assertTrue(report.errorCount() >= 2, () -> report.issues().toString());
        assertTrue(report.issues().stream().anyMatch(i -> i.file().endsWith("syntax.yml")));
        assertTrue(report.issues().stream().anyMatch(i -> "nomat".equals(i.id()) && "material".equals(i.parameter())));
    }

    @Test
    void multipleItemsPerFileAreSupported() throws IOException {
        DirectoryLayout layout = emptyLayout();
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("many.yml"), """
                first:
                  material: STONE
                second:
                  material: DIRT
                """);
        ConfigReport report = new ConfigReport();
        ItemRegistry registry = new ConfigLoader(layout, m -> true, m -> true).load(report);
        assertNotNull(registry.get("first"));
        assertNotNull(registry.get("second"));
    }

    @Test
    void duplicateIdsAreReportedAndFirstWins() throws IOException {
        DirectoryLayout layout = emptyLayout();
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("a.yml"), "id: same\nmaterial: STONE\n");
        Files.writeString(layout.folder(ItemCategory.ITEM).resolve("b.yml"), "id: same\nmaterial: DIRT\n");
        ConfigReport report = new ConfigReport();
        ItemRegistry registry = new ConfigLoader(layout, m -> true, m -> true).load(report);
        assertEquals(1, registry.size());
        assertEquals(1, report.errorCount());
        assertEquals("same", report.issues().get(0).id());
    }
}
