package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerDTO;
import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerResponse;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.service.ComplaintService;
import com.crime.reporting.crime_reporting_backend.service.PoliceOfficerService;
import com.crime.reporting.crime_reporting_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/police")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('POLICE_OFFICER')")
public class PoliceOfficerController {

    private final PoliceOfficerService policeOfficerService;
    private final ComplaintService complaintService;
    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPoliceStats(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = userOpt.get();
        PoliceOfficerDTO officerDTO = policeOfficerService.getOfficerByUserId(user.getId());
        
        // Example stats - replace with real data in production
        Map<String, Object> stats = new HashMap<>();
        stats.put("assignedComplaints", 8);
        stats.put("pendingComplaints", 5);
        stats.put("resolvedComplaints", 3);
        stats.put("totalCases", 12);
        stats.put("activeCases", 7);
        stats.put("closedCases", 5);
        stats.put("badgeNumber", officerDTO.getBadgeNumber());
        stats.put("departmentName", officerDTO.getDepartmentName());
        stats.put("rank", officerDTO.getRank());
        stats.put("recentComplaints", complaintService.getComplaintsAssignedToOfficer(officerDTO.getId(), 0, 5));
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/complaints/assigned")
    public ResponseEntity<List<ComplaintDTO>> getAssignedComplaints(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = userOpt.get();
        PoliceOfficerDTO officerDTO = policeOfficerService.getOfficerByUserId(user.getId());
        
        List<ComplaintDTO> assignedComplaints = complaintService.getComplaintsAssignedToOfficer(officerDTO.getId(), 0, 100);
        return ResponseEntity.ok(assignedComplaints);
    }
    
    @PostMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintDTO> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        
        String email = authentication.getName();
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = userOpt.get();
        PoliceOfficerDTO officerDTO = policeOfficerService.getOfficerByUserId(user.getId());
        
        // Verify the complaint is assigned to this officer
        ComplaintDTO complaintDTO = complaintService.getComplaintById(id);
        if (!complaintDTO.getAssignedOfficerId().equals(officerDTO.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        // Update the status
        ComplaintDTO updatedComplaint = complaintService.updateComplaintStatus(id, status, notes);
        return ResponseEntity.ok(updatedComplaint);
    }
} 