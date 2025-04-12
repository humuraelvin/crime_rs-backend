package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Crime type is required")
    private CrimeType crimeType;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String location;
} 