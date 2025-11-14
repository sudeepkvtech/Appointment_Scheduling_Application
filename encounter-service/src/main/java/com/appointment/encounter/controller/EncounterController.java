package com.appointment.encounter.controller;

import com.appointment.encounter.entity.Encounter;
import com.appointment.encounter.service.EncounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for encounter management
 */
@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
@Tag(name = "Encounters", description = "Clinical encounter management APIs")
public class EncounterController {

    private final EncounterService encounterService;

    @GetMapping("/{id}")
    @Operation(summary = "Get encounter by ID")
    public ResponseEntity<Encounter> getEncounter(@PathVariable UUID id) {
        Encounter encounter = encounterService.getEncounter(id);
        return ResponseEntity.ok(encounter);
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Get encounter by appointment ID")
    public ResponseEntity<Encounter> getEncounterByAppointment(@PathVariable UUID appointmentId) {
        Encounter encounter = encounterService.getEncounterByAppointment(appointmentId);
        return ResponseEntity.ok(encounter);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all encounters for a patient")
    public ResponseEntity<List<Encounter>> getEncountersByPatient(@PathVariable UUID patientId) {
        List<Encounter> encounters = encounterService.getEncountersByPatient(patientId);
        return ResponseEntity.ok(encounters);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update encounter")
    public ResponseEntity<Encounter> updateEncounter(
            @PathVariable UUID id,
            @RequestBody Encounter updates) {
        Encounter encounter = encounterService.updateEncounter(id, updates);
        return ResponseEntity.ok(encounter);
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "Sign and finish encounter")
    public ResponseEntity<Encounter> signEncounter(
            @PathVariable UUID id,
            @RequestParam String signature) {
        Encounter encounter = encounterService.signEncounter(id, signature);
        return ResponseEntity.ok(encounter);
    }
}
