package com.appointment.masterdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resource entity (Provider/Practitioner)
 * Represents doctors, nurses, therapists, etc.
 */
@Entity
@Table(name = "resources", indexes = {
    @Index(name = "idx_resource_code", columnList = "code"),
    @Index(name = "idx_resource_active", columnList = "is_active"),
    @Index(name = "idx_resource_specialty", columnList = "specialty")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", unique = true, nullable = false)
    private String code; // 'DR-001', 'NURSE-023'

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "title")
    private String title; // 'Dr.', 'RN', 'PT', etc.

    @Column(name = "specialty")
    private String specialty; // 'Cardiology', 'Pediatrics', etc.

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qualifications", columnDefinition = "jsonb")
    private List<String> qualifications; // ["MD - Harvard", "Board Certified"]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "jsonb")
    private List<String> languages; // ["English", "Spanish"]

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
