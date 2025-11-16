package com.appointment.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an appointment is completed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentCompletedEvent extends AppointmentEvent {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String resourceId;
    private LocalDateTime completedAt;
    private String encounterId;
    private boolean followUpRequired;

    public AppointmentCompletedEvent(String eventId, String appointmentId,
                                     String patientId, String resourceId,
                                     LocalDateTime completedAt, String encounterId,
                                     boolean followUpRequired) {
        super(eventId, appointmentId, "APPOINTMENT_COMPLETED");
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.completedAt = completedAt;
        this.encounterId = encounterId;
        this.followUpRequired = followUpRequired;
    }
}
