package com.appointment.shared.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class for all appointment-related events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AppointmentCreatedEvent.class, name = "APPOINTMENT_CREATED"),
    @JsonSubTypes.Type(value = AppointmentConfirmedEvent.class, name = "APPOINTMENT_CONFIRMED"),
    @JsonSubTypes.Type(value = AppointmentCancelledEvent.class, name = "APPOINTMENT_CANCELLED"),
    @JsonSubTypes.Type(value = AppointmentRescheduledEvent.class, name = "APPOINTMENT_RESCHEDULED"),
    @JsonSubTypes.Type(value = AppointmentCheckedInEvent.class, name = "APPOINTMENT_CHECKED_IN"),
    @JsonSubTypes.Type(value = AppointmentCompletedEvent.class, name = "APPOINTMENT_COMPLETED")
})
public abstract class AppointmentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String appointmentId;
    private LocalDateTime eventTimestamp;
    private String eventType;

    public AppointmentEvent(String eventId, String appointmentId, String eventType) {
        this.eventId = eventId;
        this.appointmentId = appointmentId;
        this.eventTimestamp = LocalDateTime.now();
        this.eventType = eventType;
    }
}
