package com.appointment.masterdata.controller;

import com.appointment.masterdata.entity.AppointmentType;
import com.appointment.masterdata.service.AppointmentTypeService;
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
 * REST Controller for Appointment Types
 */
@RestController
@RequestMapping("/api/v1/appointment-types")
@RequiredArgsConstructor
@Tag(name = "Appointment Types", description = "Appointment type management APIs")
public class AppointmentTypeController {

    private final AppointmentTypeService appointmentTypeService;

    @PostMapping
    @Operation(summary = "Create a new appointment type")
    public ResponseEntity<AppointmentType> createAppointmentType(@Valid @RequestBody AppointmentType appointmentType) {
        AppointmentType created = appointmentTypeService.createAppointmentType(appointmentType);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all appointment types")
    public ResponseEntity<List<AppointmentType>> getAllAppointmentTypes(
            @RequestParam(required = false) Boolean activeOnly) {
        List<AppointmentType> types = activeOnly != null && activeOnly
            ? appointmentTypeService.getActiveAppointmentTypes()
            : appointmentTypeService.getAllAppointmentTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment type by ID")
    public ResponseEntity<AppointmentType> getAppointmentType(@PathVariable UUID id) {
        AppointmentType appointmentType = appointmentTypeService.getAppointmentType(id);
        return ResponseEntity.ok(appointmentType);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get appointment type by code")
    public ResponseEntity<AppointmentType> getAppointmentTypeByCode(@PathVariable String code) {
        AppointmentType appointmentType = appointmentTypeService.getAppointmentTypeByCode(code);
        return ResponseEntity.ok(appointmentType);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get appointment types by category")
    public ResponseEntity<List<AppointmentType>> getAppointmentTypesByCategory(@PathVariable String category) {
        List<AppointmentType> types = appointmentTypeService.getAppointmentTypesByCategory(category);
        return ResponseEntity.ok(types);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment type")
    public ResponseEntity<AppointmentType> updateAppointmentType(
            @PathVariable UUID id,
            @RequestBody AppointmentType updates) {
        AppointmentType updated = appointmentTypeService.updateAppointmentType(id, updates);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete appointment type")
    public ResponseEntity<Void> deleteAppointmentType(@PathVariable UUID id) {
        appointmentTypeService.deleteAppointmentType(id);
        return ResponseEntity.noContent().build();
    }
}
