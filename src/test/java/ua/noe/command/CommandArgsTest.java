package ua.noe.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandArgsTest {

    @Test
    void filterIsCaseInsensitivePrefixMatch() {
        List<String> options = List.of("give", "GUI", "list", "info");
        assertEquals(List.of("give", "GUI"), CommandArgs.filter(options, "g"));
        assertEquals(List.of("give"), CommandArgs.filter(options, "GI"));
        assertEquals(options, CommandArgs.filter(options, ""));
        assertTrue(CommandArgs.filter(options, "x").isEmpty());
    }

    @Test
    void parsesAmounts() {
        assertEquals(OptionalInt.of(5), CommandArgs.parseAmount("5", 64));
        assertEquals(OptionalInt.of(64), CommandArgs.parseAmount(" 64 ", 64));
        assertTrue(CommandArgs.parseAmount("0", 64).isEmpty());
        assertTrue(CommandArgs.parseAmount("65", 64).isEmpty());
        assertTrue(CommandArgs.parseAmount("-1", 64).isEmpty());
        assertTrue(CommandArgs.parseAmount("abc", 64).isEmpty());
    }

    @Test
    void parsesPages() {
        assertEquals(1, CommandArgs.parsePage(null));
        assertEquals(1, CommandArgs.parsePage("x"));
        assertEquals(1, CommandArgs.parsePage("0"));
        assertEquals(3, CommandArgs.parsePage("3"));
    }

    @Test
    void countsPages() {
        assertEquals(1, CommandArgs.pageCount(0, 10));
        assertEquals(1, CommandArgs.pageCount(10, 10));
        assertEquals(2, CommandArgs.pageCount(11, 10));
    }
}
