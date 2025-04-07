package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WitnessStatementResponse {
    private Long id;
    private Long caseFileId;
    private String witnessName;
    private String witnessContact;
    private String statement;
    private LocalDateTime submittedAt;
    private Boolean isAnonymous;
    private Long userId;
    private String userFullName;
} 