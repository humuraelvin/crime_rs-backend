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
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDTO {
    private Long id;
    private String title;
    private String description;
    private String location;
    private ComplaintStatus status;
    private LocalDateTime dateFiled;
    private LocalDateTime updatedAt;
    private CrimeType crimeType;
    private String incidentDate;
    private Long userId;
    private String userName; // User's full name
    private String userContact; // User's phone or email
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private List<EvidenceDTO> evidences;
} 