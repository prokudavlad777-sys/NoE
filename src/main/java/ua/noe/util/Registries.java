package ua.noe.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/** Single place for server registry lookups, so API changes only need fixing here. */
public final class Registries {

    private Registries() {
    }

    private static NamespacedKey key(String name, String... stripPrefixes) {
        String n = name.toLowerCase(Locale.ROOT).trim();
        for (String prefix : stripPrefixes) {
            if (n.startsWith(prefix)) {
                n = n.substring(prefix.length());
            }
        }
        if (n.indexOf(':') >= 0) {
            return NamespacedKey.fromString(n);
        }
        return NamespacedKey.minecraft(n);
    }

    public static Enchantment enchantment(String name) {
        NamespacedKey k = key(name);
        return k == null ? null : Registry.ENCHANTMENT.get(k);
    }

    public static Attribute attribute(String name) {
        NamespacedKey k = key(name, "generic.", "player.", "generic_");
        return k == null ? null : Registry.ATTRIBUTE.get(k);
    }

    public static PotionEffectType effect(String name) {
        NamespacedKey k = key(name);
        return k == null ? null : Registry.EFFECT.get(k);
    }
}
