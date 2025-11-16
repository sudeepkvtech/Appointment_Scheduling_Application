package com.appointment.masterdata.repository;

import com.appointment.masterdata.entity.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AppointmentType entity
 */
@Repository
public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, UUID> {

    Optional<AppointmentType> findByCode(String code);

    List<AppointmentType> findByIsActiveTrue();

    List<AppointmentType> findByCategory(String category);
}
