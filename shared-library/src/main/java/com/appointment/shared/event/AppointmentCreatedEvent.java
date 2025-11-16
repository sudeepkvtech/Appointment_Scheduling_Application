package com.appointment.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a new appointment is created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppointmentCreatedEvent extends AppointmentEvent {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String resourceId;
    private String locationId;
    private String appointmentTypeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private boolean isVirtual;
    private String chiefComplaint;

    public AppointmentCreatedEvent(String eventId, String appointmentId,
                                   String patientId, String resourceId,
                                   String locationId, String appointmentTypeId,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   String status, boolean isVirtual, String chiefComplaint) {
        super(eventId, appointmentId, "APPOINTMENT_CREATED");
        this.patientId = patientId;
        this.resourceId = resourceId;
        this.locationId = locationId;
        this.appointmentTypeId = appointmentTypeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.isVirtual = isVirtual;
        this.chiefComplaint = chiefComplaint;
    }
}
