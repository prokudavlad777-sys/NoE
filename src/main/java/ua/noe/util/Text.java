package ua.noe.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Text helpers: legacy '&' colours to Adventure components. */
public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component color(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    /** Colour a string and remove the default italic used by item names and lore. */
    public static Component item(String text) {
        return color(text).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> items(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(item(line));
        }
        return out;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String stripColors(String text) {
        return plain(color(text));
    }

    public static String replace(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            result = result.replace("%" + e.getKey() + "%", e.getValue());
        }
        return result;
    }
}
