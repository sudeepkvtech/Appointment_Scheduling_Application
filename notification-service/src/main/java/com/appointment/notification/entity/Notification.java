package com.appointment.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification entity for tracking sent notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_appointment", columnList = "appointment_id"),
    @Index(name = "idx_notification_patient", columnList = "patient_id"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_scheduled", columnList = "scheduled_for")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "type", nullable = false)
    private String type; // CONFIRMATION, REMINDER, CANCELLATION, RESCHEDULE

    @Column(name = "channel", nullable = false)
    private String channel; // EMAIL, SMS, IN_APP

    @Column(name = "recipient")
    private String recipient; // Email address or phone number

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, SENT, FAILED, CANCELLED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
