package com.sandhya.applytrack;
import java.time.LocalDate;

/** Immutable application record; all changes produce a validated replacement. */
public final class Application {
    public final long id;
    public final String company, role, notes;
    public final LocalDate appliedOn, followUpOn;
    public final Status status;
    public Application(long id, String company, String role, LocalDate appliedOn,
                       Status status, LocalDate followUpOn, String notes) {
        if (id < 1) throw new IllegalArgumentException("ID must be positive");
        if (appliedOn == null || status == null) throw new IllegalArgumentException("Date and status are required");
        if (followUpOn != null && followUpOn.isBefore(appliedOn))
            throw new IllegalArgumentException("Follow-up cannot be before the application date");
        this.id = id; this.company = text(company, "Company", true);
        this.role = text(role, "Role", true); this.notes = text(notes, "Notes", false);
        this.appliedOn = appliedOn; this.status = status; this.followUpOn = followUpOn;
    }
    private static String text(String s, String name, boolean required) {
        if (s == null || (required && s.trim().isEmpty())) throw new IllegalArgumentException(name + " is required");
        if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) throw new IllegalArgumentException(name + " must be one line");
        return s.trim();
    }
    public Application update(Status status, LocalDate followUpOn, String notes) {
        return new Application(id, company, role, appliedOn, status, followUpOn, notes);
    }
    public boolean isDue(LocalDate asOf) {
        return !status.isClosed() && followUpOn != null && !followUpOn.isAfter(asOf);
    }
}
