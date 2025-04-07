package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
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
public class ComplaintResponse {
    private Long id;
    private Long userId;
    private String userName;
    private CrimeType crimeType;
    private String description;
    private ComplaintStatus status;
    private LocalDateTime dateFiled;
    private LocalDateTime dateLastUpdated;
    private Double latitude;
    private Double longitude;
    private String location;
    private Integer priorityScore;
    private List<EvidenceResponse> evidences;
    private CaseFileResponse caseFile;
} 