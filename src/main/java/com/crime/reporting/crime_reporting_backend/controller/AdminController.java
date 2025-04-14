package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.*;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.service.AdminService;
import com.crime.reporting.crime_reporting_backend.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final ComplaintService complaintService;
    
    // Statistics endpoints
    
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        log.info("Fetching admin dashboard statistics");
        return ResponseEntity.ok(adminService.getAdminStats());
    }
    
    // Department endpoints
    
    @PostMapping("/departments")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        log.info("Creating new department: {}", request.getName());
        DepartmentResponse response = adminService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/departments/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        log.info("Updating department with id: {}", id);
        DepartmentResponse response = adminService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        log.info("Deleting department with id: {}", id);
        adminService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/departments/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        log.info("Fetching department with id: {}", id);
        DepartmentResponse response = adminService.getDepartmentById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        log.info("Fetching all departments");
        List<DepartmentResponse> departments = adminService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }
    
    @GetMapping("/departments/paged")
    public ResponseEntity<Page<DepartmentResponse>> getAllDepartmentsPaged(Pageable pageable) {
        log.info("Fetching paged departments");
        Page<DepartmentResponse> departments = adminService.getAllDepartmentsPaged(pageable);
        return ResponseEntity.ok(departments);
    }
    
    // Police officer endpoints
    
    @PostMapping("/officers")
    public ResponseEntity<PoliceOfficerResponse> createPoliceOfficer(@Valid @RequestBody PoliceOfficerRequest request) {
        log.info("Creating new police officer");
        PoliceOfficerResponse response = adminService.createPoliceOfficer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/officers/{id}")
    public ResponseEntity<PoliceOfficerResponse> updatePoliceOfficer(
            @PathVariable Long id,
            @Valid @RequestBody PoliceOfficerRequest request) {
        log.info("Updating police officer with id: {}", id);
        PoliceOfficerResponse response = adminService.updatePoliceOfficer(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/officers/{id}")
    public ResponseEntity<Void> deletePoliceOfficer(@PathVariable Long id) {
        log.info("Deleting police officer with id: {}", id);
        adminService.deletePoliceOfficer(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/officers/{id}")
    public ResponseEntity<PoliceOfficerResponse> getPoliceOfficerById(@PathVariable Long id) {
        log.info("Fetching police officer with id: {}", id);
        PoliceOfficerResponse response = adminService.getPoliceOfficerById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/officers")
    public ResponseEntity<List<PoliceOfficerResponse>> getAllPoliceOfficers() {
        log.info("Fetching all police officers");
        List<PoliceOfficerResponse> officers = adminService.getAllPoliceOfficers();
        return ResponseEntity.ok(officers);
    }
    
    @GetMapping("/officers/paged")
    public ResponseEntity<Page<PoliceOfficerResponse>> getAllPoliceOfficersPaged(Pageable pageable) {
        log.info("Fetching paged police officers");
        Page<PoliceOfficerResponse> officers = adminService.getAllPoliceOfficersPaged(pageable);
        return ResponseEntity.ok(officers);
    }
    
    @GetMapping("/departments/{departmentId}/officers")
    public ResponseEntity<Page<PoliceOfficerResponse>> getPoliceOfficersByDepartment(
            @PathVariable Long departmentId,
            Pageable pageable) {
        log.info("Fetching police officers for department with id: {}", departmentId);
        Page<PoliceOfficerResponse> officers = adminService.getPoliceOfficersByDepartment(departmentId, pageable);
        return ResponseEntity.ok(officers);
    }
    
    // Complaint management endpoints
    
    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintDTO>> getAdminComplaints() {
        log.info("Admin fetching all complaints");
        List<ComplaintDTO> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(complaints);
    }
    
    @GetMapping("/complaints/all")
    public ResponseEntity<List<ComplaintDTO>> getAllAdminComplaints() {
        log.info("Admin fetching all complaints with details");
        List<ComplaintDTO> complaints = complaintService.getAllComplaintsForAdmin();
        return ResponseEntity.ok(complaints);
    }
    
    @PostMapping("/complaints/{complaintId}/assign/{officerId}")
    public ResponseEntity<ComplaintDTO> assignComplaintToOfficer(
            @PathVariable Long complaintId,
            @PathVariable Long officerId) {
        log.info("Assigning complaint with id: {} to officer with id: {}", complaintId, officerId);
        ComplaintDTO updatedComplaint = complaintService.assignComplaintToOfficer(complaintId, officerId);
        return ResponseEntity.ok(updatedComplaint);
    }
    
    @PostMapping("/complaints/{complaintId}/assign")
    public ResponseEntity<ComplaintDTO> assignComplaintWithData(
            @PathVariable Long complaintId,
            @RequestBody AssignmentRequest request) {
        log.info("Assigning complaint with id: {} to officer with id: {}", complaintId, request.getOfficerId());
        ComplaintDTO updatedComplaint = complaintService.assignComplaintToOfficer(complaintId, request.getOfficerId());
        return ResponseEntity.ok(updatedComplaint);
    }
    
    // User management endpoints
    
    @GetMapping("/users")
    public ResponseEntity<Page<UserListResponse>> getAllUsers(Pageable pageable) {
        log.info("Fetching all users (paged)");
        Page<UserListResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/role/{role}")
    public ResponseEntity<Page<UserListResponse>> getUsersByRole(
            @PathVariable String role,
            Pageable pageable) {
        log.info("Fetching users with role: {}", role);
        Page<UserListResponse> users = adminService.getUsersByRole(role, pageable);
        return ResponseEntity.ok(users);
    }
} 