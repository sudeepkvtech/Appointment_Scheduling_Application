package com.appointment.encounter.entity;

import com.appointment.shared.enums.EncounterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Encounter entity for clinical documentation
 */
@Entity
@Table(name = "encounters", indexes = {
    @Index(name = "idx_encounter_appointment", columnList = "appointment_id"),
    @Index(name = "idx_encounter_patient", columnList = "patient_id"),
    @Index(name = "idx_encounter_resource", columnList = "resource_id"),
    @Index(name = "idx_encounter_number", columnList = "encounter_number"),
    @Index(name = "idx_encounter_date", columnList = "encounter_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "encounter_number", unique = true, nullable = false)
    private String encounterNumber;

    @Column(name = "appointment_id", unique = true, nullable = false)
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "encounter_type", nullable = false)
    private String encounterType; // OUTPATIENT, INPATIENT, EMERGENCY, etc.

    @Column(name = "encounter_date", nullable = false)
    private LocalDateTime encounterDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EncounterStatus status = EncounterStatus.PLANNED;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "history_of_present_illness", columnDefinition = "TEXT")
    private String historyOfPresentIllness;

    @Column(name = "physical_examination", columnDefinition = "TEXT")
    private String physicalExamination;

    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vital_signs", columnDefinition = "jsonb")
    private Map<String, Object> vitalSigns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_codes", columnDefinition = "jsonb")
    private Map<String, Object> diagnosisCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "procedure_codes", columnDefinition = "jsonb")
    private Map<String, Object> procedureCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medications_prescribed", columnDefinition = "jsonb")
    private Map<String, Object> medicationsPrescribed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lab_orders", columnDefinition = "jsonb")
    private Map<String, Object> labOrders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "imaging_orders", columnDefinition = "jsonb")
    private Map<String, Object> imagingOrders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "referrals", columnDefinition = "jsonb")
    private Map<String, Object> referrals;

    @Column(name = "follow_up_required")
    private Boolean followUpRequired = false;

    @Column(name = "follow_up_in_days")
    private Integer followUpInDays;

    @Column(name = "follow_up_instructions", columnDefinition = "TEXT")
    private String followUpInstructions;

    @Column(name = "provider_signature")
    private String providerSignature;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
