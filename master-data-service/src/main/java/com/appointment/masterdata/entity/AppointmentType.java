package com.appointment.masterdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AppointmentType entity
 * Defines different types of appointments (Consultation, Follow-up, Surgery, etc.)
 */
@Entity
@Table(name = "appointment_types", indexes = {
    @Index(name = "idx_appt_type_code", columnList = "code"),
    @Index(name = "idx_appt_type_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name; // "Initial Consultation", "Follow-up Visit", etc.

    @Column(name = "code", unique = true, nullable = false)
    private String code; // "CONSULT-INIT", "FOLLOW-UP"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // Default duration (30, 60, etc.)

    @Column(name = "color")
    private String color; // Hex color for calendar display, e.g., '#FF5733'

    @Column(name = "requires_preparation")
    private Boolean requiresPreparation = false;

    @Column(name = "preparation_instructions", columnDefinition = "TEXT")
    private String preparationInstructions;

    @Column(name = "is_virtual_available")
    private Boolean isVirtualAvailable = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "category")
    private String category; // 'Medical', 'Dental', 'Therapy', etc.

    @Column(name = "default_price", precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    @Column(name = "requires_referral")
    private Boolean requiresReferral = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
