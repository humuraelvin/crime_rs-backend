package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long citizenCount;
    private long policeOfficerCount;
    private long adminCount;
    
    private long totalComplaints;
    private long activeComplaints;
    private long resolvedComplaints;
    
    private long totalDepartments;
    private long activeCases;
    
    private Map<String, Long> complaintsByStatus;
    private Map<String, Long> complaintsByDepartment;
    private Map<String, Long> complaintsByMonth;
} 