package com.crime.reporting.crime_reporting_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseNoteRequest {
    @NotBlank
    private String content;
    
    private boolean isPrivate;
    
    @NotBlank
    private String caseId;
    
    public static class CaseNoteRequestBuilder {
        private boolean isPrivate = false;
    }
} 