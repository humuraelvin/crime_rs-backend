package com.crime.reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseNoteRequest {
    @NotNull
    private Long complaintId;
    
    @NotBlank
    private String note;
    
    @Builder.Default
    private boolean internal = false;
} 