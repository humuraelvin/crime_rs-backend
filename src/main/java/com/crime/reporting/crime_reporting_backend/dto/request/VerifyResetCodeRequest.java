package com.crime.reporting.crime_reporting_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyResetCodeRequest {
    private String email;
    private String resetCode;
} 