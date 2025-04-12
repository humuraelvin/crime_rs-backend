package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.ComplaintRequest;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintResponse;
import com.crime.reporting.crime_reporting_backend.dto.EvidenceResponse;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.service.ComplaintService;
import com.crime.reporting.crime_reporting_backend.service.EvidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final EvidenceService evidenceService;

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        request.setUserId(currentUser.getId());
        return new ResponseEntity<>(complaintService.createComplaint(request), HttpStatus.CREATED);
    }
    
    @PostMapping(value = "/with-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintResponse> createComplaintWithEvidence(
            @RequestPart("type") String type,
            @RequestPart("description") String description,
            @RequestPart("location") String location,
            @RequestPart(value = "priority", required = false) String priority,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {
        try {
            ComplaintRequest request = new ComplaintRequest();
            request.setCrimeType(CrimeType.valueOf(type));
            request.setDescription(description);
            request.setLocation(location);
            // Priority is now calculated automatically by the AiPrioritizationService
            // The manual priority parameter is ignored
            request.setUserId(currentUser.getId());
            
            return new ResponseEntity<>(
                complaintService.createComplaintWithFiles(request, files), 
                HttpStatus.CREATED
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ComplaintResponse>> getAllComplaints(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) CrimeType crimeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(complaintService.getAllComplaints(status, crimeType, startDate, endDate, pageable));
    }

    @GetMapping("/my-complaints")
    public ResponseEntity<Page<ComplaintResponse>> getMyComplaints(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(complaintService.getComplaintsByUser(currentUser.getId(), pageable));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status));
    }
    
    @PostMapping(value = "/upload-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<EvidenceResponse>> uploadEvidence(
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

    @GetMapping("/stats/by-status")
    public ResponseEntity<List<StatusCountDTO>> getComplaintCountsByStatus() {
        return ResponseEntity.ok(complaintService.getComplaintCountsByStatus());
    }

    @GetMapping("/stats/by-crime-type")
    public ResponseEntity<List<CrimeTypeCountDTO>> getComplaintCountsByCrimeType() {
        return ResponseEntity.ok(complaintService.getComplaintCountsByCrimeType());
    }

    @GetMapping("/stats/trends")
    public ResponseEntity<List<DateCountDTO>> getComplaintTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(complaintService.getComplaintTrends(startDate, endDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }
    
    // Inner DTOs for statistics endpoints
    public record StatusCountDTO(ComplaintStatus status, long count) {}
    public record CrimeTypeCountDTO(CrimeType crimeType, long count) {}
    public record DateCountDTO(LocalDateTime date, long count) {}
} 