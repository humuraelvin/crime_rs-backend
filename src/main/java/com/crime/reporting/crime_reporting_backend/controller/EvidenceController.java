package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.EvidenceResponse;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;

    @PostMapping("/upload")
    public ResponseEntity<List<EvidenceResponse>> uploadEvidenceFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("complaintId") Long complaintId,
            @AuthenticationPrincipal User currentUser) {
        try {
            List<EvidenceResponse> uploadedFiles = evidenceService.uploadEvidenceFiles(
                    files, complaintId, currentUser.getId());
            return new ResponseEntity<>(uploadedFiles, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/complaint/{complaintId}")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceByComplaintId(
            @PathVariable Long complaintId) {
        List<EvidenceResponse> evidences = evidenceService.getEvidenceByComplaintId(complaintId);
        return ResponseEntity.ok(evidences);
    }

    @DeleteMapping("/{evidenceId}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable Long evidenceId) {
        evidenceService.deleteEvidence(evidenceId);
        return ResponseEntity.noContent().build();
    }
} 