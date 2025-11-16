package com.appointment.scheduling.entity;

import com.appointment.shared.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment entity
 */
@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_patient", columnList = "patient_id"),
    @Index(name = "idx_appointment_resource", columnList = "resource_id"),
    @Index(name = "idx_appointment_location", columnList = "location_id"),
    @Index(name = "idx_appointment_start_time", columnList = "start_time"),
    @Index(name = "idx_appointment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_number", unique = true, nullable = false)
    private String appointmentNumber;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "appointment_type_id", nullable = false)
    private UUID appointmentTypeId;

    @Column(name = "time_slot_id")
    private UUID timeSlotId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "is_virtual")
    private Boolean isVirtual = false;

    @Column(name = "virtual_meeting_url")
    private String virtualMeetingUrl;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "patient_notes", columnDefinition = "TEXT")
    private String patientNotes;

    @Column(name = "staff_notes", columnDefinition = "TEXT")
    private String staffNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "confirmation_sent_at")
    private LocalDateTime confirmationSentAt;

    @Column(name = "is_rescheduled")
    private Boolean isRescheduled = false;

    @Column(name = "rescheduled_from_appointment_id")
    private UUID rescheduledFromAppointmentId;

    @Column(name = "rescheduled_to_appointment_id")
    private UUID rescheduledToAppointmentId;

    @Column(name = "rescheduled_at")
    private LocalDateTime rescheduledAt;

    @Column(name = "rescheduled_by")
    private UUID rescheduledBy;

    @Column(name = "reschedule_reason", columnDefinition = "TEXT")
    private String rescheduleReason;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
