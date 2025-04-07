package com.crime.reporting.crime_reporting_backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for reverse geocoding requests
 * 
 * @param latitude The latitude coordinate
 * @param longitude The longitude coordinate
 */
public record CoordinatesRequest(
    @NotNull(message = "Latitude is required")
    Double latitude,
    
    @NotNull(message = "Longitude is required")
    Double longitude
) {} 