package com.crime.reporting.crime_reporting_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WitnessStatementRequest {
    @NotBlank
    private String statement;
    
    @Builder.Default
    private boolean anonymous = false;
    
    @NotBlank
    private String caseId;
    
    @NotBlank
    private String witnessName;
    
    @NotBlank
    private String witnessContact;
} 