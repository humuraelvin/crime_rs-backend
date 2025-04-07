package com.crime.reporting.crime_reporting_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO for specifying hotspot analysis parameters
 * 
 * @param startDate Start date for the analysis period
 * @param endDate End date for the analysis period
 * @param minClusterSize Minimum number of crimes to consider a location a hotspot
 */
public record HotspotAnalysisRequest(
    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime startDate,
    
    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime endDate,
    
    @NotNull(message = "Minimum cluster size is required")
    @Min(value = 2, message = "Minimum cluster size must be at least 2")
    Integer minClusterSize
) {} 