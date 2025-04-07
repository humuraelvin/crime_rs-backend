package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.CaseStatus;
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
public class CaseFileRequest {
    @NotNull(message = "Complaint ID is required")
    private Long complaintId;
    
    private Long assignedOfficerId;
    
    private CaseStatus status;
    
    @Size(max = 5000, message = "Report summary must be less than 5000 characters")
    private String reportSummary;
    
    @Size(max = 5000, message = "Closing report must be less than 5000 characters")
    private String closingReport;
} 