package com.crime.reporting.crime_reporting_backend.dto;

/**
 * DTO for geocoding operation results
 * 
 * @param latitude The latitude coordinate
 * @param longitude The longitude coordinate
 * @param formattedAddress The formatted address
 */
public record GeocodeResponse(
    Double latitude,
    Double longitude,
    String formattedAddress
) {}