package com.appointment.scheduling.repository;

import com.appointment.scheduling.entity.TimeSlot;
import com.appointment.shared.enums.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TimeSlot entity
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {

    List<TimeSlot> findByResourceId(UUID resourceId);

    List<TimeSlot> findByStatus(TimeSlotStatus status);

    @Query("SELECT t FROM TimeSlot t WHERE t.resourceId = :resourceId " +
           "AND t.status = :status " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    List<TimeSlot> findAvailableSlots(
        @Param("resourceId") UUID resourceId,
        @Param("status") TimeSlotStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM TimeSlot t WHERE t.locationId = :locationId " +
           "AND t.status = :status " +
           "AND t.startTime >= :startDate AND t.endTime <= :endDate")
    List<TimeSlot> findAvailableSlotsByLocation(
        @Param("locationId") UUID locationId,
        @Param("status") TimeSlotStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TimeSlot t " +
           "WHERE t.resourceId = :resourceId " +
           "AND ((t.startTime < :endTime AND t.endTime > :startTime))")
    boolean existsOverlappingSlot(
        @Param("resourceId") UUID resourceId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
