package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for transferring police officer data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoliceOfficerDTO {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String badgeNumber;
    private Long departmentId;
    private String departmentName;
    private String rank;
    private String specialization;
    private String contactInfo;
    private String jurisdiction;
    private Integer activeCasesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 