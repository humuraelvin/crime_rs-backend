package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseNoteResponse {
    private Long id;
    private Long caseFileId;
    private Long authorId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
    private Boolean isPrivate;
}