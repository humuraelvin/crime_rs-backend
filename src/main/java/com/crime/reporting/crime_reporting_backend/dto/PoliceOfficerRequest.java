package com.crime.reporting.crime_reporting_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoliceOfficerRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Badge number is required")
    private String badgeNumber;
    
    @NotBlank(message = "Department is required")
    private String department;
    
    @NotBlank(message = "Rank is required")
    private String rank;
    
    private String specialization;
    
    private String contactInfo;
    
    private String jurisdiction;
} 