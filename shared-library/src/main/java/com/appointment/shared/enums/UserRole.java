package com.appointment.shared.enums;

/**
 * Enum for user roles
 */
public enum UserRole {
    PATIENT("Patient"),
    PROVIDER("Provider"),
    ADMIN("Admin"),
    STAFF("Staff");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
