package ua.noe.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigReportTest {

    @Test
    void errorMessagesContainFileIdParameterReasonAndFix() {
        ConfigReport report = new ConfigReport();
        report.error("items/a.yml", "a", "material", "bad material", "use DIAMOND_SWORD");
        String text = report.issues().get(0).format();
        assertTrue(text.contains("items/a.yml"));
        assertTrue(text.contains("a"));
        assertTrue(text.contains("material"));
        assertTrue(text.contains("bad material"));
        assertTrue(text.contains("use DIAMOND_SWORD"));
    }

    @Test
    void countsSeveritiesSeparately() {
        ConfigReport report = new ConfigReport();
        assertFalse(report.hasErrors());
        report.warn("f", "i", "p", "r", "x");
        assertFalse(report.hasErrors());
        report.error("f", "i", "p", "r", "x");
        assertTrue(report.hasErrors());
        assertEquals(1, report.errorCount());
        assertEquals(1, report.warningCount());
    }
}
