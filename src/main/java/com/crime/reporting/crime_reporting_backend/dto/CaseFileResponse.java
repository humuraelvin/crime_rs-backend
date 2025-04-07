package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseFileResponse {
    private Long id;
    private Long complaintId;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private String badgeNumber;
    private String department;
    private CaseStatus status;
    private String reportSummary;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private LocalDateTime closedAt;
    private String closingReport;
    private List<CaseNoteResponse> caseNotes;
    private List<WitnessStatementResponse> witnessStatements;
}