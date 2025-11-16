package com.appointment.shared.enums;

/**
 * Enum for encounter status
 */
public enum EncounterStatus {
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    FINISHED("Finished"),
    CANCELLED("Cancelled");

    private final String displayName;

    EncounterStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
