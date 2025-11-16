package com.appointment.masterdata.controller;

import com.appointment.masterdata.entity.Resource;
import com.appointment.masterdata.service.ResourceService;
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
 * REST Controller for Resources (Providers/Practitioners)
 */
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Resource (provider/practitioner) management APIs")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @Operation(summary = "Create a new resource")
    public ResponseEntity<Resource> createResource(@Valid @RequestBody Resource resource) {
        Resource created = resourceService.createResource(resource);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all resources")
    public ResponseEntity<List<Resource>> getAllResources(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String specialty) {
        List<Resource> resources;

        if (specialty != null && activeOnly != null && activeOnly) {
            resources = resourceService.getActiveResourcesBySpecialty(specialty);
        } else if (specialty != null) {
            resources = resourceService.getResourcesBySpecialty(specialty);
        } else if (activeOnly != null && activeOnly) {
            resources = resourceService.getActiveResources();
        } else {
            resources = resourceService.getAllResources();
        }

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<Resource> getResource(@PathVariable UUID id) {
        Resource resource = resourceService.getResource(id);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get resource by code")
    public ResponseEntity<Resource> getResourceByCode(@PathVariable String code) {
        Resource resource = resourceService.getResourceByCode(code);
        return ResponseEntity.ok(resource);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update resource")
    public ResponseEntity<Resource> updateResource(
            @PathVariable UUID id,
            @RequestBody Resource updates) {
        Resource updated = resourceService.updateResource(id, updates);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete resource")
    public ResponseEntity<Void> deleteResource(@PathVariable UUID id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
