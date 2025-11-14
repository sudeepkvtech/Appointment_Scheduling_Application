package com.appointment.scheduling.repository;

import com.appointment.scheduling.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Waitlist entity
 */
@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, UUID> {

    List<Waitlist> findByPatientId(UUID patientId);

    List<Waitlist> findByStatusOrderByPriorityDescCreatedAtAsc(String status);

    List<Waitlist> findByAppointmentTypeIdAndStatus(UUID appointmentTypeId, String status);
}
