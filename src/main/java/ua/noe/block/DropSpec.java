package ua.noe.block;

/**
 * One drop entry. {@code ref} is {@code self}, a NoE item id or a vanilla material name.
 * Syntax in YAML: {@code ref}, {@code ref:amount} or {@code ref:min-max}.
 */
public record DropSpec(String ref, int min, int max) {

    public static final String SELF = "self";
}
