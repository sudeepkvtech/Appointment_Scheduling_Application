package com.appointment.masterdata.service;

import com.appointment.masterdata.entity.Location;
import com.appointment.masterdata.repository.LocationRepository;
import com.appointment.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing locations (facilities/clinics)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional
    public Location createLocation(Location location) {
        log.info("Creating location: {}", location.getName());
        return locationRepository.save(location);
    }

    public Location getLocation(UUID id) {
        return locationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
    }

    public Location getLocationByCode(String code) {
        return locationRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Location", "code", code));
    }

    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    public List<Location> getActiveLocations() {
        return locationRepository.findByIsActiveTrue();
    }

    public List<Location> getLocationsByCity(String city) {
        return locationRepository.findByCity(city);
    }

    public List<Location> getLocationsByState(String state) {
        return locationRepository.findByState(state);
    }

    @Transactional
    public Location updateLocation(UUID id, Location updates) {
        Location location = getLocation(id);

        if (updates.getName() != null) {
            location.setName(updates.getName());
        }
        if (updates.getAddressLine1() != null) {
            location.setAddressLine1(updates.getAddressLine1());
        }
        if (updates.getAddressLine2() != null) {
            location.setAddressLine2(updates.getAddressLine2());
        }
        if (updates.getCity() != null) {
            location.setCity(updates.getCity());
        }
        if (updates.getState() != null) {
            location.setState(updates.getState());
        }
        if (updates.getZipCode() != null) {
            location.setZipCode(updates.getZipCode());
        }
        if (updates.getPhone() != null) {
            location.setPhone(updates.getPhone());
        }
        if (updates.getEmail() != null) {
            location.setEmail(updates.getEmail());
        }
        if (updates.getIsActive() != null) {
            location.setIsActive(updates.getIsActive());
        }
        if (updates.getOperatingHours() != null) {
            location.setOperatingHours(updates.getOperatingHours());
        }

        log.info("Updated location: {}", id);
        return locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(UUID id) {
        Location location = getLocation(id);
        locationRepository.delete(location);
        log.info("Deleted location: {}", id);
    }
}
