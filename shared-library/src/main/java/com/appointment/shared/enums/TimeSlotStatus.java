package com.appointment.shared.enums;

/**
 * Enum for time slot status
 */
public enum TimeSlotStatus {
    AVAILABLE("Available"),
    BOOKED("Booked"),
    BLOCKED("Blocked"),
    TENTATIVE("Tentative");

    private final String displayName;

    TimeSlotStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
