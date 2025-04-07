package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String badgeNumber;
    private String department;
    private String rank;
    private String specialization;
    private String contactInfo;
    private String jurisdiction;
    private int activeCasesCount;
}