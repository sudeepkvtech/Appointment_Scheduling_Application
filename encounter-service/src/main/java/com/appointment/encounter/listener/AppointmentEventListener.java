package com.appointment.encounter.listener;

import com.appointment.encounter.service.EncounterService;
import com.appointment.shared.event.AppointmentCancelledEvent;
import com.appointment.shared.event.AppointmentCheckedInEvent;
import com.appointment.shared.event.AppointmentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka event listener for appointment events
 * This service reacts to appointment events and manages encounters accordingly
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private final EncounterService encounterService;

    /**
     * Listen to AppointmentConfirmedEvent and create encounter
     * This is the primary trigger for encounter creation
     */
    @KafkaListener(
        topics = "appointment-events",
        groupId = "encounter-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAppointmentConfirmed(
            @Payload AppointmentConfirmedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("Received AppointmentConfirmedEvent for appointment: {} from partition: {}",
                 event.getAppointmentId(), partition);

        try {
            encounterService.createEncounterFromAppointment(
                UUID.fromString(event.getAppointmentId()),
                UUID.fromString(event.getPatientId()),
                UUID.fromString(event.getResourceId()),
                null, // Location ID not in confirmed event
                event.getStartTime(),
                null  // Chief complaint not in confirmed event
            );

            log.info("Successfully created encounter for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to create encounter for appointment: {}. Error: {}",
                      event.getAppointmentId(), e.getMessage(), e);
            // In production, you might want to publish to a dead-letter queue
        }
    }

    /**
     * Listen to AppointmentCheckedInEvent and start encounter
     */
    @KafkaListener(
        topics = "appointment-events",
        groupId = "encounter-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAppointmentCheckedIn(
            @Payload AppointmentCheckedInEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("Received AppointmentCheckedInEvent for appointment: {} from partition: {}",
                 event.getAppointmentId(), partition);

        try {
            encounterService.startEncounter(UUID.fromString(event.getAppointmentId()));
            log.info("Successfully started encounter for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to start encounter for appointment: {}. Error: {}",
                      event.getAppointmentId(), e.getMessage(), e);
        }
    }

    /**
     * Listen to AppointmentCancelledEvent and cancel encounter
     */
    @KafkaListener(
        topics = "appointment-events",
        groupId = "encounter-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAppointmentCancelled(
            @Payload AppointmentCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("Received AppointmentCancelledEvent for appointment: {} from partition: {}",
                 event.getAppointmentId(), partition);

        try {
            encounterService.cancelEncounter(UUID.fromString(event.getAppointmentId()));
            log.info("Successfully cancelled encounter for appointment: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to cancel encounter for appointment: {}. Error: {}",
                      event.getAppointmentId(), e.getMessage(), e);
        }
    }
}
