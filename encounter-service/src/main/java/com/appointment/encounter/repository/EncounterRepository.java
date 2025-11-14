package com.appointment.encounter.repository;

import com.appointment.encounter.entity.Encounter;
import com.appointment.shared.enums.EncounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Encounter entity
 */
@Repository
public interface EncounterRepository extends JpaRepository<Encounter, UUID> {

    Optional<Encounter> findByAppointmentId(UUID appointmentId);

    Optional<Encounter> findByEncounterNumber(String encounterNumber);

    List<Encounter> findByPatientId(UUID patientId);

    List<Encounter> findByResourceId(UUID resourceId);

    List<Encounter> findByStatus(EncounterStatus status);

    List<Encounter> findByPatientIdAndStatus(UUID patientId, EncounterStatus status);
}
