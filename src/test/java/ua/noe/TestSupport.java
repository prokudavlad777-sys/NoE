package ua.noe;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.noe.config.ConfigReport;
import ua.noe.config.ItemParser;
import ua.noe.item.ItemCategory;
import ua.noe.item.NoEItem;

/** Shared helpers; none of them need a running server. */
public final class TestSupport {

    private TestSupport() {
    }

    public static YamlConfiguration yaml(String text) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(text);
        } catch (InvalidConfigurationException ex) {
            throw new IllegalArgumentException(ex);
        }
        return cfg;
    }

    public static ItemParser parser(ConfigReport report) {
        return new ItemParser(report, m -> true, m -> true);
    }

    public static NoEItem parse(ItemCategory category, String yamlText, ConfigReport report) {
        return parser(report).parse(category, "test.yml", "fallback", yaml(yamlText));
    }
}
