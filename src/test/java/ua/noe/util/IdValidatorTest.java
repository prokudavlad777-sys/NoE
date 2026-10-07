package ua.noe.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdValidatorTest {

    @Test
    void acceptsTypicalIds() {
        assertTrue(IdValidator.isValid("tiger_sword"));
        assertTrue(IdValidator.isValid("revolver"));
        assertTrue(IdValidator.isValid("a1-b2_c3"));
    }

    @Test
    void rejectsBadIds() {
        assertFalse(IdValidator.isValid(null));
        assertFalse(IdValidator.isValid(""));
        assertFalse(IdValidator.isValid("Tiger_Sword"));
        assertFalse(IdValidator.isValid("_leading"));
        assertFalse(IdValidator.isValid("has space"));
        assertFalse(IdValidator.isValid("noe:item"));
        assertFalse(IdValidator.isValid("x".repeat(65)));
    }
}
