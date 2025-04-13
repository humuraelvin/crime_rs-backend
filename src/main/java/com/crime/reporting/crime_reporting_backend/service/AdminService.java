package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {
    // Department management
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
    void deleteDepartment(Long id);
    DepartmentResponse getDepartmentById(Long id);
    List<DepartmentResponse> getAllDepartments();
    Page<DepartmentResponse> getAllDepartmentsPaged(Pageable pageable);
    
    // Police officer management
    PoliceOfficerResponse createPoliceOfficer(PoliceOfficerRequest request);
    PoliceOfficerResponse updatePoliceOfficer(Long id, PoliceOfficerRequest request);
    void deletePoliceOfficer(Long id);
    PoliceOfficerResponse getPoliceOfficerById(Long id);
    List<PoliceOfficerResponse> getAllPoliceOfficers();
    Page<PoliceOfficerResponse> getAllPoliceOfficersPaged(Pageable pageable);
    Page<PoliceOfficerResponse> getPoliceOfficersByDepartment(Long departmentId, Pageable pageable);
    
    // Complaint management (assignment)
    void assignComplaintToOfficer(Long complaintId, Long officerId);
    
    // User management
    Page<UserListResponse> getAllUsers(Pageable pageable);
    Page<UserListResponse> getUsersByRole(String role, Pageable pageable);
    
    // Statistics
    AdminStatsResponse getAdminStats();
} 