package com.appointment.scheduling.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for rescheduling an appointment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleAppointmentRequest {

    @NotNull(message = "New start time is required")
    @Future(message = "New start time must be in the future")
    private LocalDateTime newStartTime;

    @NotNull(message = "New end time is required")
    @Future(message = "New end time must be in the future")
    private LocalDateTime newEndTime;

    private UUID newResourceId;

    private UUID newLocationId;

    @NotNull(message = "Reschedule reason is required")
    private String reason;

    private UUID rescheduledBy;
}
