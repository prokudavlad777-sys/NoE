package ua.noe.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {

    private final MessageService svc = MessageService.fromYaml("""
            prefix: "[P] "
            hello: "Hi %name%, %count% items"
            help:
              - "line %x%"
              - "second"
            """);

    @Test
    void replacesPlaceholders() {
        assertEquals("Hi Bob, 3 items", svc.raw("hello", "name", "Bob", "count", "3"));
    }

    @Test
    void listsAreSupported() {
        assertEquals(List.of("line 1", "second"), svc.rawList("help", "x", "1"));
    }

    @Test
    void missingKeysDoNotThrow() {
        assertTrue(svc.raw("nope").contains("nope"));
        assertTrue(svc.rawList("nope").get(0).contains("nope"));
    }
}
