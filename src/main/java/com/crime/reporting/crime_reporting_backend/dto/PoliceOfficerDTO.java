package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for transferring police officer data
 */
@Data
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
    private String rank;
    private String specialization;
    private Long departmentId;
    private String departmentName;
    private String contactInfo;
    private String jurisdiction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 