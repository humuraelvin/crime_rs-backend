package com.crime.reporting.crime_reporting_backend.dto;

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
public class CaseNoteRequest {
    @NotNull(message = "Case file ID is required")
    private Long caseFileId;
    
    @NotBlank(message = "Content is required")
    @Size(min = 5, max = 2000, message = "Content must be between 5 and 2000 characters")
    private String content;
    
    private Boolean isPrivate = true;
    
    // This will be populated by the server using the authenticated user
    private Long authorId;
}