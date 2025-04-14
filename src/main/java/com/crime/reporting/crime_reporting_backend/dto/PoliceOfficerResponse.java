package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning police officer data in API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoliceOfficerResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String badgeNumber;
    private String rank;
    private String specialization;
    private Long departmentId;
    private String departmentName;
    private String contactInfo;
    private String jurisdiction;
    private int activeCasesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}