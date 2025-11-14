package com.appointment.encounter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Encounter Service Application
 * Manages clinical encounters and listens to appointment events
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
public class EncounterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EncounterServiceApplication.java, args);
    }
}
