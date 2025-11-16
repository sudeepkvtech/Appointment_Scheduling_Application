package com.appointment.masterdata.service;

import com.appointment.masterdata.entity.AppointmentType;
import com.appointment.masterdata.repository.AppointmentTypeRepository;
import com.appointment.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing appointment types
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentTypeService {

    private final AppointmentTypeRepository appointmentTypeRepository;

    @Transactional
    public AppointmentType createAppointmentType(AppointmentType appointmentType) {
        log.info("Creating appointment type: {}", appointmentType.getName());
        return appointmentTypeRepository.save(appointmentType);
    }

    public AppointmentType getAppointmentType(UUID id) {
        return appointmentTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("AppointmentType", "id", id));
    }

    public AppointmentType getAppointmentTypeByCode(String code) {
        return appointmentTypeRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("AppointmentType", "code", code));
    }

    public List<AppointmentType> getAllAppointmentTypes() {
        return appointmentTypeRepository.findAll();
    }

    public List<AppointmentType> getActiveAppointmentTypes() {
        return appointmentTypeRepository.findByIsActiveTrue();
    }

    public List<AppointmentType> getAppointmentTypesByCategory(String category) {
        return appointmentTypeRepository.findByCategory(category);
    }

    @Transactional
    public AppointmentType updateAppointmentType(UUID id, AppointmentType updates) {
        AppointmentType appointmentType = getAppointmentType(id);

        if (updates.getName() != null) {
            appointmentType.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            appointmentType.setDescription(updates.getDescription());
        }
        if (updates.getDurationMinutes() != null) {
            appointmentType.setDurationMinutes(updates.getDurationMinutes());
        }
        if (updates.getColor() != null) {
            appointmentType.setColor(updates.getColor());
        }
        if (updates.getCategory() != null) {
            appointmentType.setCategory(updates.getCategory());
        }
        if (updates.getDefaultPrice() != null) {
            appointmentType.setDefaultPrice(updates.getDefaultPrice());
        }
        if (updates.getIsActive() != null) {
            appointmentType.setIsActive(updates.getIsActive());
        }

        log.info("Updated appointment type: {}", id);
        return appointmentTypeRepository.save(appointmentType);
    }

    @Transactional
    public void deleteAppointmentType(UUID id) {
        AppointmentType appointmentType = getAppointmentType(id);
        appointmentTypeRepository.delete(appointmentType);
        log.info("Deleted appointment type: {}", id);
    }
}
