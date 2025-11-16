package com.appointment.masterdata.service;

import com.appointment.masterdata.entity.Resource;
import com.appointment.masterdata.repository.ResourceRepository;
import com.appointment.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing resources (providers/practitioners)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional
    public Resource createResource(Resource resource) {
        log.info("Creating resource: {} {}", resource.getFirstName(), resource.getLastName());
        return resourceRepository.save(resource);
    }

    public Resource getResource(UUID id) {
        return resourceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
    }

    public Resource getResourceByCode(String code) {
        return resourceRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", "code", code));
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> getActiveResources() {
        return resourceRepository.findByIsActiveTrue();
    }

    public List<Resource> getResourcesBySpecialty(String specialty) {
        return resourceRepository.findBySpecialty(specialty);
    }

    public List<Resource> getActiveResourcesBySpecialty(String specialty) {
        return resourceRepository.findByIsActiveTrueAndSpecialty(specialty);
    }

    @Transactional
    public Resource updateResource(UUID id, Resource updates) {
        Resource resource = getResource(id);

        if (updates.getFirstName() != null) {
            resource.setFirstName(updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            resource.setLastName(updates.getLastName());
        }
        if (updates.getTitle() != null) {
            resource.setTitle(updates.getTitle());
        }
        if (updates.getSpecialty() != null) {
            resource.setSpecialty(updates.getSpecialty());
        }
        if (updates.getEmail() != null) {
            resource.setEmail(updates.getEmail());
        }
        if (updates.getPhone() != null) {
            resource.setPhone(updates.getPhone());
        }
        if (updates.getBio() != null) {
            resource.setBio(updates.getBio());
        }
        if (updates.getIsActive() != null) {
            resource.setIsActive(updates.getIsActive());
        }

        log.info("Updated resource: {}", id);
        return resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResource(UUID id) {
        Resource resource = getResource(id);
        resourceRepository.delete(resource);
        log.info("Deleted resource: {}", id);
    }
}
