package ua.noe.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects configuration issues during loading. Not thread-safe; one report per load. */
public final class ConfigReport {

    private final List<ConfigIssue> issues = new ArrayList<>();

    public void error(String file, String id, String parameter, String reason, String fix) {
        issues.add(new ConfigIssue(ConfigIssue.Severity.ERROR, file, id, parameter, reason, fix));
    }

    public void warn(String file, String id, String parameter, String reason, String fix) {
        issues.add(new ConfigIssue(ConfigIssue.Severity.WARNING, file, id, parameter, reason, fix));
    }

    public List<ConfigIssue> issues() {
        return Collections.unmodifiableList(issues);
    }

    public int errorCount() {
        return (int) issues.stream().filter(i -> i.severity() == ConfigIssue.Severity.ERROR).count();
    }

    public int warningCount() {
        return (int) issues.stream().filter(i -> i.severity() == ConfigIssue.Severity.WARNING).count();
    }

    public boolean hasErrors() {
        return errorCount() > 0;
    }

    public void logTo(Logger logger) {
        for (ConfigIssue issue : issues) {
            logger.log(issue.severity() == ConfigIssue.Severity.ERROR ? Level.SEVERE : Level.WARNING, issue.format());
        }
    }
}
