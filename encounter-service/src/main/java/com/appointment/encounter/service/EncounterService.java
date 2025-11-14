package com.appointment.encounter.service;

import com.appointment.encounter.entity.Encounter;
import com.appointment.encounter.repository.EncounterRepository;
import com.appointment.shared.enums.EncounterStatus;
import com.appointment.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for encounter management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncounterService {

    private final EncounterRepository encounterRepository;

    /**
     * Create encounter from appointment
     */
    @Transactional
    public Encounter createEncounterFromAppointment(UUID appointmentId, UUID patientId,
                                                     UUID resourceId, UUID locationId,
                                                     LocalDateTime encounterDate,
                                                     String chiefComplaint) {
        log.info("Creating encounter for appointment: {}", appointmentId);

        // Check if encounter already exists for this appointment
        if (encounterRepository.findByAppointmentId(appointmentId).isPresent()) {
            log.warn("Encounter already exists for appointment: {}", appointmentId);
            return encounterRepository.findByAppointmentId(appointmentId).get();
        }

        Encounter encounter = new Encounter();
        encounter.setEncounterNumber(generateEncounterNumber());
        encounter.setAppointmentId(appointmentId);
        encounter.setPatientId(patientId);
        encounter.setResourceId(resourceId);
        encounter.setLocationId(locationId);
        encounter.setEncounterType("OUTPATIENT");
        encounter.setEncounterDate(encounterDate);
        encounter.setStatus(EncounterStatus.PLANNED);
        encounter.setChiefComplaint(chiefComplaint);

        Encounter saved = encounterRepository.save(encounter);
        log.info("Encounter created successfully: {}", saved.getId());

        return saved;
    }

    /**
     * Get encounter by ID
     */
    public Encounter getEncounter(UUID encounterId) {
        return encounterRepository.findById(encounterId)
            .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", encounterId));
    }

    /**
     * Get encounter by appointment ID
     */
    public Encounter getEncounterByAppointment(UUID appointmentId) {
        return encounterRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Encounter", "appointmentId", appointmentId));
    }

    /**
     * Get encounters by patient
     */
    public List<Encounter> getEncountersByPatient(UUID patientId) {
        return encounterRepository.findByPatientId(patientId);
    }

    /**
     * Update encounter status to IN_PROGRESS
     */
    @Transactional
    public Encounter startEncounter(UUID appointmentId) {
        log.info("Starting encounter for appointment: {}", appointmentId);

        Encounter encounter = encounterRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Encounter", "appointmentId", appointmentId));

        encounter.setStatus(EncounterStatus.IN_PROGRESS);
        return encounterRepository.save(encounter);
    }

    /**
     * Update encounter
     */
    @Transactional
    public Encounter updateEncounter(UUID encounterId, Encounter updates) {
        Encounter encounter = getEncounter(encounterId);

        if (updates.getHistoryOfPresentIllness() != null) {
            encounter.setHistoryOfPresentIllness(updates.getHistoryOfPresentIllness());
        }
        if (updates.getPhysicalExamination() != null) {
            encounter.setPhysicalExamination(updates.getPhysicalExamination());
        }
        if (updates.getAssessment() != null) {
            encounter.setAssessment(updates.getAssessment());
        }
        if (updates.getPlan() != null) {
            encounter.setPlan(updates.getPlan());
        }
        if (updates.getVitalSigns() != null) {
            encounter.setVitalSigns(updates.getVitalSigns());
        }
        if (updates.getDiagnosisCodes() != null) {
            encounter.setDiagnosisCodes(updates.getDiagnosisCodes());
        }
        if (updates.getProcedureCodes() != null) {
            encounter.setProcedureCodes(updates.getProcedureCodes());
        }

        return encounterRepository.save(encounter);
    }

    /**
     * Sign and finish encounter
     */
    @Transactional
    public Encounter signEncounter(UUID encounterId, String providerSignature) {
        Encounter encounter = getEncounter(encounterId);

        encounter.setProviderSignature(providerSignature);
        encounter.setSignedAt(LocalDateTime.now());
        encounter.setStatus(EncounterStatus.FINISHED);

        log.info("Encounter signed and finished: {}", encounterId);
        return encounterRepository.save(encounter);
    }

    /**
     * Cancel encounter
     */
    @Transactional
    public void cancelEncounter(UUID appointmentId) {
        encounterRepository.findByAppointmentId(appointmentId).ifPresent(encounter -> {
            encounter.setStatus(EncounterStatus.CANCELLED);
            encounterRepository.save(encounter);
            log.info("Encounter cancelled for appointment: {}", appointmentId);
        });
    }

    private String generateEncounterNumber() {
        return "ENC-" + System.currentTimeMillis();
    }
}
