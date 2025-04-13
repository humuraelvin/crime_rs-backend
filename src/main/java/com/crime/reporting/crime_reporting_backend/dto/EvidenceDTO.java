package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for transferring evidence data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceDTO {
    private Long id;
    private Long complaintId;
    private EvidenceType type;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String description;
    private String metadata;
    private LocalDateTime uploadedAt;
    private Long uploadedById;
    private String uploadedByName;
} 