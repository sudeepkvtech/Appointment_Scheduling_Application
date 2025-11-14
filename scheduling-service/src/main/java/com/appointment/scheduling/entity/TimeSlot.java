package com.appointment.scheduling.entity;

import com.appointment.shared.enums.TimeSlotStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TimeSlot entity for available appointment slots
 */
@Entity
@Table(name = "time_slots", indexes = {
    @Index(name = "idx_timeslot_resource", columnList = "resource_id"),
    @Index(name = "idx_timeslot_location", columnList = "location_id"),
    @Index(name = "idx_timeslot_start_time", columnList = "start_time"),
    @Index(name = "idx_timeslot_status", columnList = "status"),
    @Index(name = "idx_timeslot_resource_time", columnList = "resource_id,start_time,end_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "appointment_type_id")
    private UUID appointmentTypeId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeSlotStatus status = TimeSlotStatus.AVAILABLE;

    @Column(name = "is_virtual")
    private Boolean isVirtual = false;

    @Column(name = "max_bookings")
    private Integer maxBookings = 1;

    @Column(name = "current_bookings")
    private Integer currentBookings = 0;

    @Column(name = "block_reason")
    private String blockReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
