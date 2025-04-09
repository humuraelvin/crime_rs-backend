package com.crime.reporting.crime_reporting_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisableMfaRequest {
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
    private String password;
} 