package ua.noe.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.noe.util.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads messages.yml once and keeps the strings in memory. */
public final class MessageService {

    private volatile Map<String, String> single = Map.of();
    private volatile Map<String, List<String>> multi = Map.of();
    private volatile String prefix = "";

    public void load(FileConfiguration cfg) {
        Map<String, String> s = new HashMap<>();
        Map<String, List<String>> m = new HashMap<>();
        for (String key : cfg.getKeys(true)) {
            if (cfg.isList(key)) {
                m.put(key, cfg.getStringList(key));
            } else if (cfg.isString(key)) {
                s.put(key, cfg.getString(key, ""));
            }
        }
        this.single = Map.copyOf(s);
        this.multi = Map.copyOf(m);
        this.prefix = s.getOrDefault("prefix", "");
    }

    public static MessageService fromYaml(String yaml) {
        MessageService svc = new MessageService();
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid messages YAML", ex);
        }
        svc.load(cfg);
        return svc;
    }

    /** Raw message with placeholders applied. {@code pairs} = key1, value1, key2, value2... */
    public String raw(String key, String... pairs) {
        String text = single.get(key);
        if (text == null) {
            return "&cMissing message: " + key;
        }
        return Text.replace(text, toMap(pairs));
    }

    public List<String> rawList(String key, String... pairs) {
        List<String> lines = multi.get(key);
        if (lines == null) {
            return List.of("&cMissing message: " + key);
        }
        Map<String, String> map = toMap(pairs);
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(Text.replace(line, map));
        }
        return out;
    }

    public Component component(String key, String... pairs) {
        return Text.color(prefix + raw(key, pairs));
    }

    public void send(Audience target, String key, String... pairs) {
        target.sendMessage(component(key, pairs));
    }

    public void sendList(Audience target, String key, String... pairs) {
        for (String line : rawList(key, pairs)) {
            target.sendMessage(Text.color(line));
        }
    }

    private static Map<String, String> toMap(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
