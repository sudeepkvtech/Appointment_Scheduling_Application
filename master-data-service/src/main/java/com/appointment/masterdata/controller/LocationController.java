package com.appointment.masterdata.controller;

import com.appointment.masterdata.entity.Location;
import com.appointment.masterdata.service.LocationService;
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
 * REST Controller for Locations (Facilities/Clinics)
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Location (facility/clinic) management APIs")
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @Operation(summary = "Create a new location")
    public ResponseEntity<Location> createLocation(@Valid @RequestBody Location location) {
        Location created = locationService.createLocation(location);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all locations")
    public ResponseEntity<List<Location>> getAllLocations(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state) {
        List<Location> locations;

        if (city != null) {
            locations = locationService.getLocationsByCity(city);
        } else if (state != null) {
            locations = locationService.getLocationsByState(state);
        } else if (activeOnly != null && activeOnly) {
            locations = locationService.getActiveLocations();
        } else {
            locations = locationService.getAllLocations();
        }

        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID")
    public ResponseEntity<Location> getLocation(@PathVariable UUID id) {
        Location location = locationService.getLocation(id);
        return ResponseEntity.ok(location);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get location by code")
    public ResponseEntity<Location> getLocationByCode(@PathVariable String code) {
        Location location = locationService.getLocationByCode(code);
        return ResponseEntity.ok(location);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update location")
    public ResponseEntity<Location> updateLocation(
            @PathVariable UUID id,
            @RequestBody Location updates) {
        Location updated = locationService.updateLocation(id, updates);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete location")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
