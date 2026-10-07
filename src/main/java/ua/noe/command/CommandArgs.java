package ua.noe.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

/** Pure helpers for command argument handling (unit-tested without a server). */
public final class CommandArgs {

    private CommandArgs() {
    }

    /** Options starting with the prefix, case-insensitive, original order. */
    public static List<String> filter(Collection<String> options, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(option);
            }
        }
        return out;
    }

    /** Parses an amount in 1..max; empty if invalid. */
    public static OptionalInt parseAmount(String raw, int max) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 1 && value <= max ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    /** Parses a 1-based page number, falling back to 1. */
    public static int parsePage(String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public static int pageCount(int total, int perPage) {
        return Math.max(1, (total + perPage - 1) / perPage);
    }
}
