package ua.noe.config;

/** One configuration problem, with everything needed to fix it. */
public record ConfigIssue(Severity severity, String file, String id, String parameter, String reason, String fix) {

    public enum Severity { WARNING, ERROR }

    public String format() {
        return severity + " | file: " + file
                + " | id: " + (id == null ? "-" : id)
                + " | parameter: " + (parameter == null ? "-" : parameter)
                + " | reason: " + reason
                + " | fix: " + fix;
    }
}
