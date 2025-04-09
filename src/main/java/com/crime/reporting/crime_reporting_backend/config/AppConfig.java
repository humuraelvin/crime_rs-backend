package com.crime.reporting.crime_reporting_backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@Configuration
@ConditionalOnProperty(name = "spring.session.store-type", havingValue = "simple")
@EnableAutoConfiguration
public class AppConfig {
    // Configuration for simple session storage
    // No Redis-related beans needed
} 