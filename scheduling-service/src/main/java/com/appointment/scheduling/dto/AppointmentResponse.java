package com.appointment.scheduling.dto;

import com.appointment.shared.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for appointment response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private UUID id;
    private String appointmentNumber;
    private UUID patientId;
    private UUID resourceId;
    private UUID locationId;
    private UUID appointmentTypeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private Boolean isVirtual;
    private String virtualMeetingUrl;
    private String chiefComplaint;
    private String notes;
    private String patientNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
