package com.appointment.masterdata.repository;

import com.appointment.masterdata.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Location entity
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    Optional<Location> findByCode(String code);

    List<Location> findByIsActiveTrue();

    List<Location> findByCity(String city);

    List<Location> findByState(String state);
}
