package com.crime.reporting.crime_reporting_backend.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    
    private String firstName;
    
    private String lastName;
    
    @Pattern(regexp = "^[0-9+]{10,}$", message = "Phone number must be at least 10 digits")
    private String phoneNumber;
} 