package com.appointment.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security Configuration for API Gateway
 *
 * IMPORTANT: This configuration currently allows all requests for development.
 * To enable JWT authentication:
 * 1. Uncomment the JWT filter configuration
 * 2. Create an Identity Service or authentication endpoint
 * 3. Update the filter to validate tokens
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF for stateless API
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Configure authorization
                .authorizeExchange(exchange -> exchange
                        // Allow all requests for now (development mode)
                        .anyExchange().permitAll()

                        // TO ENABLE AUTHENTICATION, replace the above with:
                        // .pathMatchers("/api/v1/auth/**").permitAll()
                        // .pathMatchers("/actuator/**").permitAll()
                        // .pathMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        // .anyExchange().authenticated()
                )

                .build();
    }

    /**
     * Uncomment this to enable JWT authentication
     */
    /*
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                          AuthenticationFilter authFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
    */
}
