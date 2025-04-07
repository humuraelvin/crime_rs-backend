package com.crime.reporting.crime_reporting_backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for address geocoding requests
 * 
 * @param address The address to geocode
 */
public record AddressRequest(
    @NotBlank(message = "Address is required")
    String address
) {} 