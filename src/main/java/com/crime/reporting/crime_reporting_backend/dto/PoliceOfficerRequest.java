package com.crime.reporting.crime_reporting_backend.dto;

import jakarta.validation.constraints.Email;
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
    // User info - for creating a new user along with police officer
    private String firstName;
    private String lastName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String password;
    private String phoneNumber;
    
    // Officer specific info
    @NotBlank(message = "Badge number is required")
    private String badgeNumber;
    
    @NotNull(message = "Department ID is required")
    private Long departmentId;
    
    @NotBlank(message = "Rank is required")
    private String rank;
    
    private String specialization;
    
    private String contactInfo;
    
    private String jurisdiction;
} 