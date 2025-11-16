package com.appointment.scheduling.service;

import com.appointment.scheduling.dto.AppointmentResponse;
import com.appointment.scheduling.dto.CancelAppointmentRequest;
import com.appointment.scheduling.dto.CreateAppointmentRequest;
import com.appointment.scheduling.dto.RescheduleAppointmentRequest;
import com.appointment.scheduling.entity.Appointment;
import com.appointment.scheduling.entity.AppointmentHistory;
import com.appointment.scheduling.entity.TimeSlot;
import com.appointment.scheduling.repository.AppointmentHistoryRepository;
import com.appointment.scheduling.repository.AppointmentRepository;
import com.appointment.scheduling.repository.TimeSlotRepository;
import com.appointment.shared.enums.AppointmentStatus;
import com.appointment.shared.enums.TimeSlotStatus;
import com.appointment.shared.event.*;
import com.appointment.shared.exception.BadRequestException;
import com.appointment.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for appointment management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final EventPublisher eventPublisher;

    /**
     * Create a new appointment
     */
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for patient: {}", request.getPatientId());

        // Validate time range
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        // Check for conflicting appointments
        if (appointmentRepository.existsConflictingAppointment(
                request.getResourceId(),
                request.getStartTime(),
                request.getEndTime())) {
            throw new BadRequestException("Resource already has a conflicting appointment at this time");
        }

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setAppointmentNumber(generateAppointmentNumber());
        appointment.setPatientId(request.getPatientId());
        appointment.setResourceId(request.getResourceId());
        appointment.setLocationId(request.getLocationId());
        appointment.setAppointmentTypeId(request.getAppointmentTypeId());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setIsVirtual(request.getIsVirtual());
        appointment.setChiefComplaint(request.getChiefComplaint());
        appointment.setNotes(request.getNotes());
        appointment.setPatientNotes(request.getPatientNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Update time slot if exists
        updateTimeSlotStatus(request.getResourceId(), request.getStartTime(),
                           request.getEndTime(), TimeSlotStatus.BOOKED);

        // Create history entry
        createHistoryEntry(savedAppointment.getId(), "CREATED", null, null, "Appointment created");

        // Publish event
        publishAppointmentCreatedEvent(savedAppointment);

        log.info("Appointment created successfully: {}", savedAppointment.getId());

        return mapToResponse(savedAppointment);
    }

    /**
     * Get appointment by ID
     */
    public AppointmentResponse getAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        return mapToResponse(appointment);
    }

    /**
     * Get all appointments for a patient
     */
    public List<AppointmentResponse> getAppointmentsByPatient(UUID patientId) {
        return appointmentRepository.findByPatientId(patientId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all appointments for a resource
     */
    public List<AppointmentResponse> getAppointmentsByResource(UUID resourceId) {
        return appointmentRepository.findByResourceId(resourceId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Confirm an appointment
     */
    @Transactional
    public AppointmentResponse confirmAppointment(UUID appointmentId, UUID confirmedBy) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Only scheduled appointments can be confirmed");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create history entry
        createHistoryEntry(appointmentId, "CONFIRMED", null, confirmedBy, "Appointment confirmed");

        // Publish event
        publishAppointmentConfirmedEvent(savedAppointment, confirmedBy);

        log.info("Appointment confirmed: {}", appointmentId);

        return mapToResponse(savedAppointment);
    }

    /**
     * Cancel an appointment
     */
    @Transactional
    public void cancelAppointment(UUID appointmentId, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.getReason());
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy(request.getCancelledBy());

        appointmentRepository.save(appointment);

        // Release time slot
        updateTimeSlotStatus(appointment.getResourceId(), appointment.getStartTime(),
                           appointment.getEndTime(), TimeSlotStatus.AVAILABLE);

        // Create history entry
        createHistoryEntry(appointmentId, "CANCELLED", request.getCancelledBy(), request.getReason());

        // Publish event
        publishAppointmentCancelledEvent(appointment, request);

        log.info("Appointment cancelled: {}", appointmentId);
    }

    /**
     * Reschedule an appointment
     */
    @Transactional
    public AppointmentResponse rescheduleAppointment(UUID appointmentId, RescheduleAppointmentRequest request) {
        Appointment oldAppointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (oldAppointment.getStatus() == AppointmentStatus.CANCELLED ||
            oldAppointment.getStatus() == AppointmentStatus.RESCHEDULED) {
            throw new BadRequestException("Cannot reschedule a cancelled or already rescheduled appointment");
        }

        // Validate new time range
        if (request.getNewEndTime().isBefore(request.getNewStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        UUID newResourceId = request.getNewResourceId() != null ?
            request.getNewResourceId() : oldAppointment.getResourceId();

        // Check for conflicts
        if (appointmentRepository.existsConflictingAppointment(
                newResourceId, request.getNewStartTime(), request.getNewEndTime())) {
            throw new BadRequestException("Resource already has a conflicting appointment at the new time");
        }

        // Create new appointment
        Appointment newAppointment = new Appointment();
        newAppointment.setAppointmentNumber(generateAppointmentNumber());
        newAppointment.setPatientId(oldAppointment.getPatientId());
        newAppointment.setResourceId(newResourceId);
        newAppointment.setLocationId(request.getNewLocationId() != null ?
            request.getNewLocationId() : oldAppointment.getLocationId());
        newAppointment.setAppointmentTypeId(oldAppointment.getAppointmentTypeId());
        newAppointment.setStartTime(request.getNewStartTime());
        newAppointment.setEndTime(request.getNewEndTime());
        newAppointment.setStatus(AppointmentStatus.SCHEDULED);
        newAppointment.setIsVirtual(oldAppointment.getIsVirtual());
        newAppointment.setChiefComplaint(oldAppointment.getChiefComplaint());
        newAppointment.setRescheduledFromAppointmentId(oldAppointment.getId());

        Appointment savedNewAppointment = appointmentRepository.save(newAppointment);

        // Update old appointment
        oldAppointment.setStatus(AppointmentStatus.RESCHEDULED);
        oldAppointment.setIsRescheduled(true);
        oldAppointment.setRescheduledToAppointmentId(savedNewAppointment.getId());
        oldAppointment.setRescheduledAt(LocalDateTime.now());
        oldAppointment.setRescheduledBy(request.getRescheduledBy());
        oldAppointment.setRescheduleReason(request.getReason());

        appointmentRepository.save(oldAppointment);

        // Release old time slot
        updateTimeSlotStatus(oldAppointment.getResourceId(), oldAppointment.getStartTime(),
                           oldAppointment.getEndTime(), TimeSlotStatus.AVAILABLE);

        // Book new time slot
        updateTimeSlotStatus(newResourceId, request.getNewStartTime(),
                           request.getNewEndTime(), TimeSlotStatus.BOOKED);

        // Create history entries
        createHistoryEntry(oldAppointment.getId(), "RESCHEDULED", request.getRescheduledBy(),
                         "Rescheduled to appointment: " + savedNewAppointment.getId());
        createHistoryEntry(savedNewAppointment.getId(), "CREATED", request.getRescheduledBy(),
                         "Created from rescheduled appointment: " + oldAppointment.getId());

        // Publish event
        publishAppointmentRescheduledEvent(oldAppointment, savedNewAppointment, request);

        log.info("Appointment rescheduled from {} to {}", appointmentId, savedNewAppointment.getId());

        return mapToResponse(savedNewAppointment);
    }

    /**
     * Check-in an appointment
     */
    @Transactional
    public AppointmentResponse checkInAppointment(UUID appointmentId, UUID checkedInBy) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED &&
            appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Only confirmed or scheduled appointments can be checked in");
        }

        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointment.setCheckedInAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create history entry
        createHistoryEntry(appointmentId, "CHECKED_IN", checkedInBy, "Patient checked in");

        // Publish event
        publishAppointmentCheckedInEvent(savedAppointment, checkedInBy);

        log.info("Appointment checked in: {}", appointmentId);

        return mapToResponse(savedAppointment);
    }

    /**
     * Complete an appointment
     */
    @Transactional
    public AppointmentResponse completeAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCheckedOutAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create history entry
        createHistoryEntry(appointmentId, "COMPLETED", null, "Appointment completed");

        // Publish event
        publishAppointmentCompletedEvent(savedAppointment);

        log.info("Appointment completed: {}", appointmentId);

        return mapToResponse(savedAppointment);
    }

    // Helper methods

    private String generateAppointmentNumber() {
        return "APT-" + System.currentTimeMillis();
    }

    private void updateTimeSlotStatus(UUID resourceId, LocalDateTime startTime,
                                     LocalDateTime endTime, TimeSlotStatus status) {
        List<TimeSlot> slots = timeSlotRepository.findAvailableSlots(
            resourceId, TimeSlotStatus.AVAILABLE, startTime, endTime);

        for (TimeSlot slot : slots) {
            if (slot.getStartTime().equals(startTime) && slot.getEndTime().equals(endTime)) {
                slot.setStatus(status);
                if (status == TimeSlotStatus.BOOKED) {
                    slot.setCurrentBookings(slot.getCurrentBookings() + 1);
                } else if (status == TimeSlotStatus.AVAILABLE) {
                    slot.setCurrentBookings(Math.max(0, slot.getCurrentBookings() - 1));
                }
                timeSlotRepository.save(slot);
                break;
            }
        }
    }

    private void createHistoryEntry(UUID appointmentId, String action,
                                    UUID changedBy, String notes) {
        AppointmentHistory history = new AppointmentHistory();
        history.setAppointmentId(appointmentId);
        history.setAction(action);
        history.setChangedBy(changedBy);
        history.setNotes(notes);
        historyRepository.save(history);
    }

    private void createHistoryEntry(UUID appointmentId, String action, UUID changedBy,
                                   UUID value, String notes) {
        createHistoryEntry(appointmentId, action, changedBy, notes);
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setAppointmentNumber(appointment.getAppointmentNumber());
        response.setPatientId(appointment.getPatientId());
        response.setResourceId(appointment.getResourceId());
        response.setLocationId(appointment.getLocationId());
        response.setAppointmentTypeId(appointment.getAppointmentTypeId());
        response.setStartTime(appointment.getStartTime());
        response.setEndTime(appointment.getEndTime());
        response.setStatus(appointment.getStatus());
        response.setIsVirtual(appointment.getIsVirtual());
        response.setVirtualMeetingUrl(appointment.getVirtualMeetingUrl());
        response.setChiefComplaint(appointment.getChiefComplaint());
        response.setNotes(appointment.getNotes());
        response.setPatientNotes(appointment.getPatientNotes());
        response.setCreatedAt(appointment.getCreatedAt());
        response.setUpdatedAt(appointment.getUpdatedAt());
        return response;
    }

    // Event publishing methods

    private void publishAppointmentCreatedEvent(Appointment appointment) {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
            UUID.randomUUID().toString(),
            appointment.getId().toString(),
            appointment.getPatientId().toString(),
            appointment.getResourceId().toString(),
            appointment.getLocationId().toString(),
            appointment.getAppointmentTypeId().toString(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            appointment.getStatus().name(),
            appointment.getIsVirtual(),
            appointment.getChiefComplaint()
        );
        eventPublisher.publishEvent(event);
    }

    private void publishAppointmentConfirmedEvent(Appointment appointment, UUID confirmedBy) {
        AppointmentConfirmedEvent event = new AppointmentConfirmedEvent(
            UUID.randomUUID().toString(),
            appointment.getId().toString(),
            appointment.getPatientId().toString(),
            appointment.getResourceId().toString(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            LocalDateTime.now(),
            confirmedBy != null ? confirmedBy.toString() : null
        );
        eventPublisher.publishEvent(event);
    }

    private void publishAppointmentCancelledEvent(Appointment appointment, CancelAppointmentRequest request) {
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
            UUID.randomUUID().toString(),
            appointment.getId().toString(),
            appointment.getPatientId().toString(),
            appointment.getResourceId().toString(),
            appointment.getStartTime(),
            request.getReason(),
            LocalDateTime.now(),
            request.getCancelledBy() != null ? request.getCancelledBy().toString() : null,
            request.getNotifyPatient()
        );
        eventPublisher.publishEvent(event);
    }

    private void publishAppointmentRescheduledEvent(Appointment oldAppointment,
                                                    Appointment newAppointment,
                                                    RescheduleAppointmentRequest request) {
        AppointmentRescheduledEvent event = new AppointmentRescheduledEvent(
            UUID.randomUUID().toString(),
            oldAppointment.getId().toString(),
            oldAppointment.getId().toString(),
            newAppointment.getId().toString(),
            oldAppointment.getPatientId().toString(),
            newAppointment.getResourceId().toString(),
            oldAppointment.getStartTime(),
            oldAppointment.getEndTime(),
            newAppointment.getStartTime(),
            newAppointment.getEndTime(),
            request.getReason(),
            LocalDateTime.now(),
            request.getRescheduledBy() != null ? request.getRescheduledBy().toString() : null
        );
        eventPublisher.publishEvent(event);
    }

    private void publishAppointmentCheckedInEvent(Appointment appointment, UUID checkedInBy) {
        AppointmentCheckedInEvent event = new AppointmentCheckedInEvent(
            UUID.randomUUID().toString(),
            appointment.getId().toString(),
            appointment.getPatientId().toString(),
            appointment.getResourceId().toString(),
            LocalDateTime.now(),
            checkedInBy != null ? checkedInBy.toString() : null
        );
        eventPublisher.publishEvent(event);
    }

    private void publishAppointmentCompletedEvent(Appointment appointment) {
        AppointmentCompletedEvent event = new AppointmentCompletedEvent(
            UUID.randomUUID().toString(),
            appointment.getId().toString(),
            appointment.getPatientId().toString(),
            appointment.getResourceId().toString(),
            LocalDateTime.now(),
            null, // encounterId will be set by Encounter Service
            false
        );
        eventPublisher.publishEvent(event);
    }
}
