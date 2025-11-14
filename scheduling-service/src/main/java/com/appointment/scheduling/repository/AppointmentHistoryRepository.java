package com.appointment.scheduling.repository;

import com.appointment.scheduling.entity.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for AppointmentHistory entity
 */
@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {

    List<AppointmentHistory> findByAppointmentIdOrderByChangedAtDesc(UUID appointmentId);
}
