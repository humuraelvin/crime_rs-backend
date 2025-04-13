package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.ApiResponse;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.service.ComplaintService;
import com.crime.reporting.crime_reporting_backend.service.PoliceOfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintAssignmentController {

    private final ComplaintService complaintService;
    private final PoliceOfficerService policeOfficerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PostMapping("/{complaintId}/assign/{officerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> assignComplaintToOfficer(
            @PathVariable Long complaintId,
            @PathVariable Long officerId) {
        
        complaintService.assignComplaintToOfficer(complaintId, officerId);
        return ResponseEntity.ok(new ApiResponse(true, "Complaint successfully assigned to officer"));
    }

    @PostMapping("/{complaintId}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> unassignComplaint(@PathVariable Long complaintId) {
        complaintService.unassignComplaint(complaintId);
        return ResponseEntity.ok(new ApiResponse(true, "Complaint successfully unassigned"));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByStatus(@PathVariable ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.getComplaintsByStatus(status));
    }
} 