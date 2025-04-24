package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintRequest;
import com.crime.reporting.crime_reporting_backend.dto.EvidenceResponse;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.service.ComplaintService;
import com.crime.reporting.crime_reporting_backend.service.EvidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import com.crime.reporting.crime_reporting_backend.dto.ErrorResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;
    private final EvidenceService evidenceService;

    @PostMapping
    public ResponseEntity<ComplaintDTO> createComplaint(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        request.setUserId(currentUser.getId());
        return new ResponseEntity<>(complaintService.createComplaint(request), HttpStatus.CREATED);
    }
    
    @PostMapping(value = "/with-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintDTO> createComplaintWithEvidence(
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
            log.error("Error creating complaint with evidence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDTO> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    @GetMapping
    public ResponseEntity<List<ComplaintDTO>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/my-complaints")
    public ResponseEntity<?> getMyComplaints(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        try {
            Page<ComplaintDTO> complaints = complaintService.getComplaintsByUser(currentUser.getId(), pageable);
            return ResponseEntity.ok(complaints);
        } catch (Exception e) {
            log.error("Error fetching complaints for user {}: {}", currentUser.getId(), e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Error fetching complaints: " + e.getMessage(),
                            "/api/v1/complaints/my-complaints")
                    );
        }
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<ComplaintDTO> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Updating complaint ID: {} by user ID: {}", id, currentUser.getId());
        
        // Verify that this complaint belongs to the current user
        ComplaintDTO existingComplaint = complaintService.getComplaintById(id);
        if (!existingComplaint.getUserId().equals(currentUser.getId())) {
            log.warn("User {} attempted to update complaint {} belonging to user {}", 
                    currentUser.getId(), id, existingComplaint.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Verify that the complaint is in an editable state (SUBMITTED status)
        if (!ComplaintStatus.SUBMITTED.name().equals(existingComplaint.getStatus())) {
            log.warn("User {} attempted to update complaint {} with non-editable status: {}", 
                    currentUser.getId(), id, existingComplaint.getStatus());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
        
        // Update the complaint with new information
        request.setUserId(currentUser.getId());
        ComplaintDTO updatedComplaint = complaintService.updateComplaintInfo(id, request);
        return ResponseEntity.ok(updatedComplaint);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintDTO> updateComplaintStatus(
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
            log.error("Error uploading evidence: {}", e.getMessage(), e);
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

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAllStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("byStatus", complaintService.getComplaintCountsByStatus());
        statistics.put("byCrimeType", complaintService.getComplaintCountsByCrimeType());
        
        // For trends, use the last 30 days by default
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30);
        statistics.put("trends", complaintService.getComplaintTrends(startDate, endDate));
        
        return ResponseEntity.ok(statistics);
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
    public static record StatusCountDTO(String status, long count) {}
    public static record CrimeTypeCountDTO(String crimeType, long count) {}
    public static record DateCountDTO(String date, long count) {}
} 