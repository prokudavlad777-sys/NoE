package ua.noe.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.noe.item.ItemCategory;
import ua.noe.item.NoEItem;
import ua.noe.registry.ItemRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reads every definition file once and produces an {@link ItemRegistry}. A broken file never throws:
 * it is reported and skipped. Safe to run off the main thread.
 */
public final class ConfigLoader {

    private final DirectoryLayout layout;
    private final Predicate<Material> isItem;
    private final Predicate<Material> isBlock;

    public ConfigLoader(DirectoryLayout layout, Predicate<Material> isItem, Predicate<Material> isBlock) {
        this.layout = layout;
        this.isItem = isItem;
        this.isBlock = isBlock;
    }

    public ItemRegistry load(ConfigReport report) {
        ItemRegistry.Builder builder = ItemRegistry.builder(report);
        ItemParser parser = new ItemParser(report, isItem, isBlock);
        for (ItemCategory category : ItemCategory.values()) {
            Path folder = layout.folder(category);
            List<Path> files;
            try (Stream<Path> walk = Files.exists(folder) ? Files.walk(folder) : Stream.<Path>empty()) {
                files = walk.filter(Files::isRegularFile)
                        .filter(p -> {
                            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            return n.endsWith(".yml") || n.endsWith(".yaml");
                        })
                        .sorted()
                        .toList();
            } catch (IOException ex) {
                report.error(category.folder() + "/", null, null, "Cannot list folder: " + ex.getMessage(),
                        "Check file permissions of plugins/NoE/" + category.folder());
                continue;
            }
            for (Path path : files) {
                loadFile(category, path, parser, builder, report);
            }
        }
        return builder.build();
    }

    private void loadFile(ItemCategory category, Path path, ItemParser parser, ItemRegistry.Builder builder,
                          ConfigReport report) {
        String display = layout.root().relativize(path).toString().replace('\\', '/');
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(path));
        } catch (IOException ex) {
            report.error(display, null, null, "Cannot read file: " + ex.getMessage(), "Check file permissions");
            return;
        } catch (InvalidConfigurationException ex) {
            report.error(display, null, null, "Invalid YAML: " + firstLine(ex.getMessage()),
                    "Fix the YAML syntax (indentation, quotes, colons)");
            return;
        }
        if (yaml.getKeys(false).isEmpty()) {
            report.warn(display, null, null, "File is empty", "Add an item definition or delete the file");
            return;
        }
        if (yaml.contains("id")) {
            add(parser.parse(category, display, stem(path), yaml), builder);
            return;
        }
        ConfigurationSection container = yaml.isConfigurationSection("items") ? yaml.getConfigurationSection("items") : yaml;
        for (String key : container.getKeys(false)) {
            ConfigurationSection sec = container.getConfigurationSection(key);
            if (sec == null) {
                report.error(display, key, null, "Entry '" + key + "' is not a section",
                        "Define items as '<id>:' followed by indented properties, or use a top-level 'id:'");
                continue;
            }
            add(parser.parse(category, display, key, sec), builder);
        }
    }

    private static void add(NoEItem item, ItemRegistry.Builder builder) {
        if (item != null) {
            builder.add(item);
        }
    }

    private static String stem(Path path) {
        String n = path.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "unknown error";
        }
        int nl = s.indexOf('\n');
        return nl > 0 ? s.substring(0, nl) : s;
    }
}
