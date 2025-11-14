package com.appointment.scheduling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Waitlist entity for patients waiting for appointment slots
 */
@Entity
@Table(name = "waitlist", indexes = {
    @Index(name = "idx_waitlist_patient", columnList = "patient_id"),
    @Index(name = "idx_waitlist_status", columnList = "status"),
    @Index(name = "idx_waitlist_priority", columnList = "priority")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_type_id", nullable = false)
    private UUID appointmentTypeId;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "preferred_date_start")
    private LocalDate preferredDateStart;

    @Column(name = "preferred_date_end")
    private LocalDate preferredDateEnd;

    @Column(name = "preferred_time_start")
    private LocalTime preferredTimeStart;

    @Column(name = "preferred_time_end")
    private LocalTime preferredTimeEnd;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE, CONTACTED, BOOKED, EXPIRED, CANCELLED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "contacted_at")
    private LocalDateTime contactedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
