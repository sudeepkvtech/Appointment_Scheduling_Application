package com.appointment.scheduling.controller;

import com.appointment.scheduling.dto.AppointmentResponse;
import com.appointment.scheduling.dto.CancelAppointmentRequest;
import com.appointment.scheduling.dto.CreateAppointmentRequest;
import com.appointment.scheduling.dto.RescheduleAppointmentRequest;
import com.appointment.scheduling.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for appointment management
 */
@RestController
@RequestMapping("/api/v1/scheduling/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment management APIs")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Create a new appointment")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable UUID id) {
        AppointmentResponse response = appointmentService.getAppointment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all appointments for a patient")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByPatient(
            @PathVariable UUID patientId) {
        List<AppointmentResponse> appointments = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/resource/{resourceId}")
    @Operation(summary = "Get all appointments for a resource")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByResource(
            @PathVariable UUID resourceId) {
        List<AppointmentResponse> appointments = appointmentService.getAppointmentsByResource(resourceId);
        return ResponseEntity.ok(appointments);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm an appointment")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID confirmedBy) {
        AppointmentResponse response = appointmentService.confirmAppointment(id, confirmedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentRequest request) {
        appointmentService.cancelAppointment(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule an appointment")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/check-in")
    @Operation(summary = "Check-in for an appointment")
    public ResponseEntity<AppointmentResponse> checkInAppointment(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID checkedInBy) {
        AppointmentResponse response = appointmentService.checkInAppointment(id, checkedInBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete an appointment")
    public ResponseEntity<AppointmentResponse> completeAppointment(@PathVariable UUID id) {
        AppointmentResponse response = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(response);
    }
}
