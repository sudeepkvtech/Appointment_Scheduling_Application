package com.appointment.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an appointment is confirmed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentConfirmedEvent extends AppointmentEvent {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String resourceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime confirmedAt;
    private String confirmedBy;

    public AppointmentConfirmedEvent(String eventId, String appointmentId,
                                     String patientId, String resourceId,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     LocalDateTime confirmedAt, String confirmedBy) {
        super(eventId, appointmentId, "APPOINTMENT_CONFIRMED");
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.confirmedAt = confirmedAt;
        this.confirmedBy = confirmedBy;
    }
}
