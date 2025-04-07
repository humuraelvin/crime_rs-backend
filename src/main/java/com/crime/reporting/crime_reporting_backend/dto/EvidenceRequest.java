package com.crime.reporting.crime_reporting_backend.dto;

import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvidenceRequest {
    @NotNull(message = "Complaint ID is required")
    private Long complaintId;
    
    @NotNull(message = "Evidence type is required")
    private EvidenceType type;
    
    // These fields will be extracted from the uploaded file
    // and populated by the service layer
    private String fileName;
    private String fileContentType;
    private Long fileSize;
    
    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;
    
    // This will be populated by the server using the authenticated user
    private Long uploadedById;
} 