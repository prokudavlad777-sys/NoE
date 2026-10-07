package ua.noe.config;

import org.bukkit.configuration.file.FileConfiguration;

/** Immutable snapshot of config.yml. */
public record NoESettings(
        boolean autoReload,
        double maxArmorProtection,
        PackSettings pack
) {

    public record PackSettings(
            boolean enabled,
            boolean autoGenerate,
            boolean sendOnJoin,
            boolean required,
            String prompt,
            int minFormat,
            int maxFormat,
            String description,
            String url,
            boolean hostEnabled,
            String hostBind,
            int hostPort,
            String hostPublicAddress
    ) {
    }

    public static NoESettings from(FileConfiguration c) {
        double maxProt = Math.max(0.0, Math.min(1.0, c.getDouble("armor.max-protection", 0.95)));
        PackSettings pack = new PackSettings(
                c.getBoolean("resource-pack.enabled", true),
                c.getBoolean("resource-pack.auto-generate", true),
                c.getBoolean("resource-pack.send-on-join", true),
                c.getBoolean("resource-pack.required", false),
                c.getString("resource-pack.prompt", ""),
                c.getInt("resource-pack.min-format", 84),
                c.getInt("resource-pack.max-format", 200),
                c.getString("resource-pack.description", "NoE generated resource pack"),
                c.getString("resource-pack.url", ""),
                c.getBoolean("resource-pack.host.enabled", false),
                c.getString("resource-pack.host.bind-address", "0.0.0.0"),
                c.getInt("resource-pack.host.port", 8765),
                c.getString("resource-pack.host.public-address", "127.0.0.1"));
        return new NoESettings(c.getBoolean("auto-reload", true), maxProt, pack);
    }
}
