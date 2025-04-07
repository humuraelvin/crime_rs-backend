package com.crime.reporting.crime_reporting_backend.dto;

import java.util.Map;

/**
 * DTO for representing crime hotspot data
 */
public record HotspotResponse(
    // Geographical center of the hotspot
    Double latitude,
    Double longitude,
    
    // Number of crime reports in the hotspot
    Integer crimeCount,
    
    // Most common crime type in the hotspot
    String dominantCrimeType,
    
    // Distribution of crime types within the hotspot
    Map<String, Long> crimeTypeCounts,
    
    // Average severity score of crimes in the hotspot
    Double averageSeverity,
    
    // Radius of the hotspot in kilometers
    Double radiusKm
) {} 