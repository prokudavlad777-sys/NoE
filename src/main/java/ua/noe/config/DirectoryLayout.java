package ua.noe.config;

import ua.noe.item.ItemCategory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;

/** The plugins/NoE directory structure. */
public final class DirectoryLayout {

    private static final List<String> DEFAULT_FILES = List.of(
            "items/tiger_sword.yml",
            "items/tiger_fang.yml",
            "items/pistol_ammo.yml",
            "weapons/revolver.yml",
            "armor/tiger_poncho.yml",
            "armor/tiger_head.yml",
            "blocks/tropical_workbench.yml",
            "food/tropical_fruit.yml");

    private final Path root;

    public DirectoryLayout(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public Path folder(ItemCategory category) {
        return root.resolve(category.folder());
    }

    public Path resourcePackDir() {
        return root.resolve("resourcepack");
    }

    public Path cacheDir() {
        return root.resolve("cache");
    }

    public Path packFile() {
        return cacheDir().resolve("NoE-pack.zip");
    }

    /** Creates every directory. Example files are copied only when the item folders did not exist yet. */
    public void install(Logger logger) {
        try {
            boolean firstRun = !Files.exists(folder(ItemCategory.ITEM));
            Files.createDirectories(root);
            for (ItemCategory c : ItemCategory.values()) {
                Files.createDirectories(folder(c));
            }
            Files.createDirectories(resourcePackDir());
            Files.createDirectories(resourcePackDir().resolve("assets/noe/textures/item"));
            Files.createDirectories(resourcePackDir().resolve("assets/noe/models/item"));
            Files.createDirectories(cacheDir());
            if (firstRun) {
                for (String file : DEFAULT_FILES) {
                    copyResource("defaults/" + file, root.resolve(file), logger);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create the NoE directory structure in " + root, ex);
        }
    }

    private void copyResource(String resource, Path target, Logger logger) throws IOException {
        try (InputStream in = DirectoryLayout.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                logger.warning("Bundled example file missing from the jar: " + resource);
                return;
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
