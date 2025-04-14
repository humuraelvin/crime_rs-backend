package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliceOfficerStatisticsDTO {
    private Long officerId;
    private String officerName;
    private int totalAssignedComplaints;
    private int pendingComplaints;
    private int inProgressComplaints;
    private int resolvedComplaints;
    private int closedComplaints;
} 