package com.crime.reporting.crime_reporting_backend.dto.request;

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
public class WitnessStatementRequest {
    @NotNull
    private Long complaintId;
    
    @NotBlank
    private String statement;
    
    @Builder.Default
    private boolean anonymous = false;
    
    @Builder.Default
    private boolean willingToTestify = false;
} 