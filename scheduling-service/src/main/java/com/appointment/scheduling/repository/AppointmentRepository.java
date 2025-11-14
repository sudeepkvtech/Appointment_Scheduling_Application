package com.appointment.scheduling.repository;

import com.appointment.scheduling.entity.Appointment;
import com.appointment.shared.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Appointment entity
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByResourceId(UUID resourceId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.resourceId = :resourceId " +
           "AND a.startTime >= :startDate AND a.endTime <= :endDate " +
           "AND a.status NOT IN ('CANCELLED', 'RESCHEDULED')")
    List<Appointment> findByResourceIdAndDateRange(
        @Param("resourceId") UUID resourceId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId " +
           "AND a.startTime >= :startDate AND a.endTime <= :endDate " +
           "AND a.status NOT IN ('CANCELLED', 'RESCHEDULED')")
    List<Appointment> findByPatientIdAndDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
           "WHERE a.resourceId = :resourceId " +
           "AND ((a.startTime < :endTime AND a.endTime > :startTime)) " +
           "AND a.status NOT IN ('CANCELLED', 'RESCHEDULED')")
    boolean existsConflictingAppointment(
        @Param("resourceId") UUID resourceId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status " +
           "AND a.startTime >= :startDate AND a.endTime <= :endDate")
    long countByStatusAndDateRange(
        @Param("status") AppointmentStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
