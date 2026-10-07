package ua.noe.util;

import java.util.regex.Pattern;

/** Validation of NoE item/block identifiers. */
public final class IdValidator {

    private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9_\\-]{0,63}$");

    private IdValidator() {
    }

    public static boolean isValid(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public static String requirementsText() {
        return "1-64 chars: lowercase letters, digits, '_' or '-', starting with a letter or digit";
    }
}
