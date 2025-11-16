package com.appointment.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an appointment is cancelled
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentCancelledEvent extends AppointmentEvent {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String resourceId;
    private LocalDateTime appointmentStartTime;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private boolean notifyPatient;

    public AppointmentCancelledEvent(String eventId, String appointmentId,
                                     String patientId, String resourceId,
                                     LocalDateTime appointmentStartTime,
                                     String cancellationReason,
                                     LocalDateTime cancelledAt, String cancelledBy,
                                     boolean notifyPatient) {
        super(eventId, appointmentId, "APPOINTMENT_CANCELLED");
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.appointmentStartTime = appointmentStartTime;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.notifyPatient = notifyPatient;
    }
}
