package com.appointment.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an appointment is rescheduled
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentRescheduledEvent extends AppointmentEvent {
    private static final long serialVersionUID = 1L;

    private String oldAppointmentId;
    private String newAppointmentId;
    private String patientId;
    private String resourceId;
    private LocalDateTime oldStartTime;
    private LocalDateTime oldEndTime;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;
    private String rescheduleReason;
    private LocalDateTime rescheduledAt;
    private String rescheduledBy;

    public AppointmentRescheduledEvent(String eventId, String appointmentId,
                                       String oldAppointmentId, String newAppointmentId,
                                       String patientId, String resourceId,
                                       LocalDateTime oldStartTime, LocalDateTime oldEndTime,
                                       LocalDateTime newStartTime, LocalDateTime newEndTime,
                                       String rescheduleReason, LocalDateTime rescheduledAt,
                                       String rescheduledBy) {
        super(eventId, appointmentId, "APPOINTMENT_RESCHEDULED");
        this.oldAppointmentId = oldAppointmentId;
        this.newAppointmentId = newAppointmentId;
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.oldStartTime = oldStartTime;
        this.oldEndTime = oldEndTime;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
        this.rescheduleReason = rescheduleReason;
        this.rescheduledAt = rescheduledAt;
        this.rescheduledBy = rescheduledBy;
    }
}
