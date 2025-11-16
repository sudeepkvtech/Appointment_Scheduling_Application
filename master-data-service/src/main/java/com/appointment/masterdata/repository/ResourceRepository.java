package com.appointment.masterdata.repository;

import com.appointment.masterdata.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Resource entity
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findByCode(String code);

    List<Resource> findByIsActiveTrue();

    List<Resource> findBySpecialty(String specialty);

    List<Resource> findByIsActiveTrueAndSpecialty(String specialty);
}
