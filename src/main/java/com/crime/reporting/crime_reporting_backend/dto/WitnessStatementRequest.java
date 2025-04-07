package com.crime.reporting.crime_reporting_backend.dto;

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
public class WitnessStatementRequest {
    @NotNull(message = "Case file ID is required")
    private Long caseFileId;
    
    @NotBlank(message = "Witness name is required")
    private String witnessName;
    
    private String witnessContact;
    
    @NotBlank(message = "Statement is required")
    @Size(min = 10, max = 3000, message = "Statement must be between 10 and 3000 characters")
    private String statement;
    
    private Boolean isAnonymous = false;
    
    // This will be populated by the server using the authenticated user if available
    private Long userId;
} 