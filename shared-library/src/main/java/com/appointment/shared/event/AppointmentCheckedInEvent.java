package com.appointment.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a patient checks in for appointment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentCheckedInEvent extends AppointmentEvent {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String resourceId;
    private LocalDateTime checkedInAt;
    private String checkedInBy;

    public AppointmentCheckedInEvent(String eventId, String appointmentId,
                                     String patientId, String resourceId,
                                     LocalDateTime checkedInAt, String checkedInBy) {
        super(eventId, appointmentId, "APPOINTMENT_CHECKED_IN");
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.checkedInAt = checkedInAt;
        this.checkedInBy = checkedInBy;
    }
}
