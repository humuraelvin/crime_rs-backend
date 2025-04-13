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
public class PoliceOfficerResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String badgeNumber;
    
    // Department details
    private Long departmentId;
    private String departmentName;
    
    private String rank;
    private String specialization;
    private String contactInfo;
    private String jurisdiction;
    private int activeCasesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}