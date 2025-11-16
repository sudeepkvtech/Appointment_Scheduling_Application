package com.appointment.masterdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Master Data Service Application
 * Manages all reference data: Appointment Types, Resources, and Locations
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MasterDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MasterDataServiceApplication.class, args);
    }
}
