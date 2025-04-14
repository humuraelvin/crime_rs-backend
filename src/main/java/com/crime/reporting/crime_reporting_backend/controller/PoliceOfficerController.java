package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerDTO;
import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerResponse;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.dto.UserResponse;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.service.UserService;
import com.crime.reporting.crime_reporting_backend.service.impl.ComplaintServiceImpl;
import com.crime.reporting.crime_reporting_backend.service.impl.PoliceOfficerServiceImpl;
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
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/police")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('POLICE_OFFICER')")
public class PoliceOfficerController {

    private final PoliceOfficerServiceImpl policeOfficerService;
    private final ComplaintServiceImpl complaintService;
    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPoliceStats(Authentication authentication) {
        log.info("Fetching police stats for user: {}", authentication.getName());
        
        try {
            String email = authentication.getName();
            UserResponse userResponse = userService.findByEmail(email);
            
            // Get officer details
            PoliceOfficerDTO officerDTO;
            try {
                officerDTO = policeOfficerService.getOfficerByUserId(userResponse.getId());
            } catch (Exception e) {
                log.error("Error fetching officer details for user ID: {}", userResponse.getId(), e);
                // Create a basic DTO with user info
                officerDTO = new PoliceOfficerDTO();
                officerDTO.setBadgeNumber("Unknown");
                officerDTO.setDepartmentName("Not Assigned");
                officerDTO.setRank("Officer");
            }
            
            // Get complaints assigned to the officer
            List<ComplaintDTO> assignedComplaints = new ArrayList<>();
            try {
                if (officerDTO.getId() != null) {
                    assignedComplaints = complaintService.getComplaintsByPoliceOfficerId(officerDTO.getId());
                }
            } catch (Exception e) {
                log.error("Error fetching assigned complaints for officer ID: {}", officerDTO.getId(), e);
            }
            
            // Calculate real statistics
            int totalAssigned = assignedComplaints.size();
            
            // Count complaints by status
            int pendingCount = 0;
            int resolvedCount = 0;
            int activeCount = 0;
            int closedCount = 0;
            
            for (ComplaintDTO complaint : assignedComplaints) {
                String status = complaint.getStatus();
                if (status == null) continue;
                
                if (status.equals("ASSIGNED") || status.equals("PENDING") || status.equals("PENDING_EVIDENCE")) {
                    pendingCount++;
                } else if (status.equals("RESOLVED") || status.equals("CLOSED")) {
                    resolvedCount++;
                    closedCount++;
                }
                
                if (status.equals("INVESTIGATING") || status.equals("UNDER_REVIEW") || 
                    status.equals("ASSIGNED") || status.equals("PENDING") || 
                    status.equals("PENDING_EVIDENCE")) {
                    activeCount++;
                }
            }
            
            // Get 5 most recent complaints
            List<ComplaintDTO> recentComplaints = assignedComplaints.stream()
                .sorted((c1, c2) -> {
                    if (c1.getDateLastUpdated() != null && c2.getDateLastUpdated() != null) {
                        return c2.getDateLastUpdated().compareTo(c1.getDateLastUpdated());
                    } else {
                        return 0;
                    }
                })
                .limit(5)
                .collect(Collectors.toList());
            
            // Create response with real stats
            Map<String, Object> stats = new HashMap<>();
            stats.put("assignedComplaints", totalAssigned);
            stats.put("pendingComplaints", pendingCount);
            stats.put("resolvedComplaints", resolvedCount);
            stats.put("totalCases", totalAssigned);
            stats.put("activeCases", activeCount);
            stats.put("closedCases", closedCount);
            stats.put("badgeNumber", officerDTO.getBadgeNumber());
            stats.put("departmentName", officerDTO.getDepartmentName());
            stats.put("rank", officerDTO.getRank());
            stats.put("recentComplaints", recentComplaints);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Unexpected error in getPoliceStats", e);
            // Return a default response in case of error
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("assignedComplaints", 0);
            defaultStats.put("pendingComplaints", 0);
            defaultStats.put("resolvedComplaints", 0);
            defaultStats.put("totalCases", 0);
            defaultStats.put("activeCases", 0);
            defaultStats.put("closedCases", 0);
            defaultStats.put("badgeNumber", "Unknown");
            defaultStats.put("departmentName", "Unknown");
            defaultStats.put("rank", "Unknown");
            defaultStats.put("recentComplaints", new ArrayList<>());
            
            return ResponseEntity.ok(defaultStats);
        }
    }
    
    @GetMapping("/complaints/assigned")
    public ResponseEntity<List<ComplaintDTO>> getAssignedComplaints(Authentication authentication) {
        log.info("Fetching assigned complaints for user: {}", authentication.getName());
        
        try {
            String email = authentication.getName();
            UserResponse userResponse = userService.findByEmail(email);
            PoliceOfficerDTO officerDTO;
            
            try {
                officerDTO = policeOfficerService.getOfficerByUserId(userResponse.getId());
                List<ComplaintDTO> assignedComplaints = complaintService.getComplaintsByPoliceOfficerId(officerDTO.getId());
                return ResponseEntity.ok(assignedComplaints);
            } catch (Exception e) {
                log.error("Error fetching assigned complaints", e);
                // Return empty list instead of error
                return ResponseEntity.ok(new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("Unexpected error in getAssignedComplaints", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
    
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintDTO> updateComplaintStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate,
            Authentication authentication) {
        
        String status = statusUpdate.get("status");
        String notes = statusUpdate.get("notes");
        
        log.info("Updating complaint status: id={}, status={}, user={}", id, status, authentication.getName());
        
        try {
            // Get the current user
            String email = authentication.getName();
            UserResponse userResponse = userService.findByEmail(email);
            PoliceOfficerDTO officerDTO = policeOfficerService.getOfficerByUserId(userResponse.getId());
            
            // Validate that this complaint is assigned to this officer
            ComplaintDTO complaint = complaintService.getComplaintById(id);
            if (complaint == null || !complaint.getAssignedOfficerId().equals(officerDTO.getId())) {
                log.warn("Unauthorized attempt to update complaint: officer {} tried to update complaint {}", 
                         officerDTO.getId(), id);
                return ResponseEntity.status(403).build();
            }
            
            // Update the complaint status
            ComplaintDTO updatedComplaint = complaintService.updateComplaintStatus(id, status, notes);
            return ResponseEntity.ok(updatedComplaint);
        } catch (Exception e) {
            log.error("Error updating complaint status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 