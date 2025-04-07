package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintRequest {
    @NotNull(message = "Crime type is required")
    private CrimeType crimeType;
    
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;
    
    private Double latitude;
    
    private Double longitude;
    
    private String location;
    
    // This will be populated by the server using the authenticated user
    private Long userId;
} 