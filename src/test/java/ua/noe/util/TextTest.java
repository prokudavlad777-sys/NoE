package ua.noe.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {

    @Test
    void replacesAllPlaceholders() {
        assertEquals("1 and 2 and 1", Text.replace("%a% and %b% and %a%", Map.of("a", "1", "b", "2")));
    }

    @Test
    void stripsLegacyColours() {
        assertEquals("Tiger Sword", Text.stripColors("&6Tiger &lSword"));
    }

    @Test
    void nullTextBecomesEmpty() {
        assertEquals("", Text.stripColors(null));
    }
}
