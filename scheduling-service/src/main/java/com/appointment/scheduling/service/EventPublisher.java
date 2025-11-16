package com.appointment.scheduling.service;

import com.appointment.shared.event.AppointmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing appointment events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private static final String TOPIC = "appointment-events";

    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;

    /**
     * Publish an appointment event to Kafka
     *
     * @param event The appointment event to publish
     */
    public void publishEvent(AppointmentEvent event) {
        log.info("Publishing event: {} for appointment: {}",
                 event.getEventType(), event.getAppointmentId());

        CompletableFuture<SendResult<String, AppointmentEvent>> future =
            kafkaTemplate.send(TOPIC, event.getAppointmentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published event: {} for appointment: {} to partition: {}",
                         event.getEventType(),
                         event.getAppointmentId(),
                         result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish event: {} for appointment: {}. Error: {}",
                          event.getEventType(),
                          event.getAppointmentId(),
                          ex.getMessage());
            }
        });
    }
}
