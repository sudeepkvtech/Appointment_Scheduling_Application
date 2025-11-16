package com.appointment.scheduling.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for creating a new appointment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Resource ID is required")
    private UUID resourceId;

    @NotNull(message = "Location ID is required")
    private UUID locationId;

    @NotNull(message = "Appointment type ID is required")
    private UUID appointmentTypeId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    private Boolean isVirtual = false;

    private String chiefComplaint;

    private String notes;

    private String patientNotes;
}
