package com.crime.reporting.crime_reporting_backend.service.impl;

import com.crime.reporting.crime_reporting_backend.dto.*;
import com.crime.reporting.crime_reporting_backend.entity.*;
import com.crime.reporting.crime_reporting_backend.exception.ResourceNotFoundException;
import com.crime.reporting.crime_reporting_backend.repository.*;
import com.crime.reporting.crime_reporting_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    
    private final DepartmentRepository departmentRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final CaseFileRepository caseFileRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Department management
    
    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department with this name already exists");
        }
        
        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .contactInfo(request.getContactInfo())
                .build();
        
        Department savedDepartment = departmentRepository.save(department);
        log.info("Created new department: {}", savedDepartment.getName());
        
        return mapToDepartmentResponse(savedDepartment);
    }
    
    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        
        // Check if name is being changed and if it's already in use
        if (!department.getName().equals(request.getName()) && 
                departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department with this name already exists");
        }
        
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setLocation(request.getLocation());
        department.setContactInfo(request.getContactInfo());
        
        Department updatedDepartment = departmentRepository.save(department);
        log.info("Updated department: {}", updatedDepartment.getName());
        
        return mapToDepartmentResponse(updatedDepartment);
    }
    
    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        
        // Check if department has officers
        if (!department.getOfficers().isEmpty()) {
            throw new IllegalStateException("Cannot delete department with assigned officers");
        }
        
        departmentRepository.delete(department);
        log.info("Deleted department with id: {}", id);
    }
    
    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        
        return mapToDepartmentResponse(department);
    }
    
    @Override
    public List<DepartmentResponse> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream()
                .map(this::mapToDepartmentResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<DepartmentResponse> getAllDepartmentsPaged(Pageable pageable) {
        Page<Department> departmentsPage = departmentRepository.findAll(pageable);
        return departmentsPage.map(this::mapToDepartmentResponse);
    }
    
    // Police officer management
    
    @Override
    @Transactional
    public PoliceOfficerResponse createPoliceOfficer(PoliceOfficerRequest request) {
        // Find department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        
        // Check if email is already in use
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        
        // Create user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.POLICE_OFFICER)
                .mfaEnabled(false)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Create police officer
        PoliceOfficer officer = PoliceOfficer.builder()
                .user(savedUser)
                .badgeNumber(request.getBadgeNumber())
                .department(department)
                .rank(request.getRank())
                .specialization(request.getSpecialization())
                .contactInfo(request.getContactInfo())
                .jurisdiction(request.getJurisdiction())
                .build();
        
        PoliceOfficer savedOfficer = policeOfficerRepository.save(officer);
        log.info("Created new police officer: {} {}", savedUser.getFirstName(), savedUser.getLastName());
        
        return mapToPoliceOfficerResponse(savedOfficer);
    }
    
    @Override
    @Transactional
    public PoliceOfficerResponse updatePoliceOfficer(Long id, PoliceOfficerRequest request) {
        PoliceOfficer officer = policeOfficerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + id));
        
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        
        // Update user info if provided
        User user = officer.getUser();
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        
        // Update email if provided and not in use by another user
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email is already in use");
            }
            user.setEmail(request.getEmail());
        }
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        userRepository.save(user);
        
        // Update officer details
        officer.setBadgeNumber(request.getBadgeNumber());
        officer.setDepartment(department);
        officer.setRank(request.getRank());
        officer.setSpecialization(request.getSpecialization());
        officer.setContactInfo(request.getContactInfo());
        officer.setJurisdiction(request.getJurisdiction());
        
        PoliceOfficer updatedOfficer = policeOfficerRepository.save(officer);
        log.info("Updated police officer with id: {}", id);
        
        return mapToPoliceOfficerResponse(updatedOfficer);
    }
    
    @Override
    @Transactional
    public void deletePoliceOfficer(Long id) {
        PoliceOfficer officer = policeOfficerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + id));
        
        // Check if officer has assigned cases
        if (!officer.getCaseFiles().isEmpty()) {
            throw new IllegalStateException("Cannot delete officer with assigned cases");
        }
        
        // Delete officer
        policeOfficerRepository.delete(officer);
        
        // Delete user
        userRepository.delete(officer.getUser());
        
        log.info("Deleted police officer with id: {}", id);
    }
    
    @Override
    public PoliceOfficerResponse getPoliceOfficerById(Long id) {
        PoliceOfficer officer = policeOfficerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + id));
        
        return mapToPoliceOfficerResponse(officer);
    }
    
    @Override
    public List<PoliceOfficerResponse> getAllPoliceOfficers() {
        List<PoliceOfficer> officers = policeOfficerRepository.findAll();
        return officers.stream()
                .map(this::mapToPoliceOfficerResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<PoliceOfficerResponse> getAllPoliceOfficersPaged(Pageable pageable) {
        Page<PoliceOfficer> officersPage = policeOfficerRepository.findAll(pageable);
        return officersPage.map(this::mapToPoliceOfficerResponse);
    }
    
    @Override
    public Page<PoliceOfficerResponse> getPoliceOfficersByDepartment(Long departmentId, Pageable pageable) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        
        Page<PoliceOfficer> officersPage = policeOfficerRepository.findByDepartment(department, pageable);
        return officersPage.map(this::mapToPoliceOfficerResponse);
    }
    
    // Complaint management
    
    @Override
    @Transactional
    public void assignComplaintToOfficer(Long complaintId, Long officerId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        
        PoliceOfficer officer = policeOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + officerId));
        
        // Create case file if it doesn't exist
        CaseFile caseFile = complaint.getCaseFile();
        if (caseFile == null) {
            caseFile = CaseFile.builder()
                    .complaint(complaint)
                    .assignedOfficer(officer)
                    .status(CaseStatus.OPEN)
                    .build();
            
            caseFileRepository.save(caseFile);
            complaint.setCaseFile(caseFile);
        } else {
            caseFile.setAssignedOfficer(officer);
            caseFileRepository.save(caseFile);
        }
        
        // Update complaint status
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaintRepository.save(complaint);
        
        log.info("Assigned complaint #{} to officer with id: {}", complaintId, officerId);
    }
    
    // User management
    
    @Override
    public Page<UserListResponse> getAllUsers(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);
        return usersPage.map(this::mapToUserListResponse);
    }
    
    @Override
    public Page<UserListResponse> getUsersByRole(String roleStr, Pageable pageable) {
        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr);
        }
        
        Page<User> usersPage = userRepository.findByRole(role, pageable);
        return usersPage.map(this::mapToUserListResponse);
    }
    
    // Statistics
    
    @Override
    public AdminStatsResponse getAdminStats() {
        // User statistics
        long totalUsers = userRepository.count();
        long citizenCount = userRepository.countByRole(Role.CITIZEN);
        long policeOfficerCount = userRepository.countByRole(Role.POLICE_OFFICER);
        long adminCount = userRepository.countByRole(Role.ADMIN);
        
        // Complaint statistics
        long totalComplaints = complaintRepository.count();
        long activeComplaints = complaintRepository.countByStatusIn(
                Arrays.asList(
                        ComplaintStatus.SUBMITTED, 
                        ComplaintStatus.UNDER_REVIEW, 
                        ComplaintStatus.ASSIGNED, 
                        ComplaintStatus.INVESTIGATING, 
                        ComplaintStatus.PENDING_EVIDENCE
                )
        );
        long resolvedComplaints = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        
        // Department statistics
        long totalDepartments = departmentRepository.count();
        
        // Case statistics
        long activeCases = caseFileRepository.countByStatus(CaseStatus.OPEN);
        
        // Complaints by status
        Map<String, Long> complaintsByStatus = new HashMap<>();
        for (ComplaintStatus status : ComplaintStatus.values()) {
            complaintsByStatus.put(status.name(), complaintRepository.countByStatus(status));
        }
        
        // Complaints by department
        Map<String, Long> complaintsByDepartment = new HashMap<>();
        List<Department> departments = departmentRepository.findAll();
        for (Department department : departments) {
            long count = caseFileRepository.countByAssignedOfficerDepartment(department);
            complaintsByDepartment.put(department.getName(), count);
        }
        
        // Complaints by month (last 6 months)
        Map<String, Long> complaintsByMonth = new HashMap<>();
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Complaint> recentComplaints = complaintRepository.findByDateFiledAfter(sixMonthsAgo);
        
        Map<Month, Long> countByMonth = recentComplaints.stream()
                .collect(Collectors.groupingBy(
                        complaint -> complaint.getDateFiled().getMonth(),
                        Collectors.counting()
                ));
        
        for (int i = 0; i < 6; i++) {
            Month month = LocalDateTime.now().minusMonths(i).getMonth();
            String monthName = month.toString().charAt(0) + month.toString().substring(1).toLowerCase();
            complaintsByMonth.put(monthName, countByMonth.getOrDefault(month, 0L));
        }
        
        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .citizenCount(citizenCount)
                .policeOfficerCount(policeOfficerCount)
                .adminCount(adminCount)
                .totalComplaints(totalComplaints)
                .activeComplaints(activeComplaints)
                .resolvedComplaints(resolvedComplaints)
                .totalDepartments(totalDepartments)
                .activeCases(activeCases)
                .complaintsByStatus(complaintsByStatus)
                .complaintsByDepartment(complaintsByDepartment)
                .complaintsByMonth(complaintsByMonth)
                .build();
    }
    
    // Helper methods for mapping entities to DTOs
    
    private DepartmentResponse mapToDepartmentResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .location(department.getLocation())
                .contactInfo(department.getContactInfo())
                .officerCount(department.getOfficers() != null ? department.getOfficers().size() : 0)
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
    
    private PoliceOfficerResponse mapToPoliceOfficerResponse(PoliceOfficer officer) {
        User user = officer.getUser();
        return PoliceOfficerResponse.builder()
                .id(officer.getId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .badgeNumber(officer.getBadgeNumber())
                .departmentId(officer.getDepartment().getId())
                .departmentName(officer.getDepartment().getName())
                .rank(officer.getRank())
                .specialization(officer.getSpecialization())
                .contactInfo(officer.getContactInfo())
                .jurisdiction(officer.getJurisdiction())
                .activeCasesCount(officer.getCaseFiles() != null ? (int) officer.getCaseFiles().stream()
                        .filter(cf -> cf.getStatus() == CaseStatus.OPEN)
                        .count() : 0)
                .createdAt(officer.getCreatedAt())
                .updatedAt(officer.getUpdatedAt())
                .build();
    }
    
    private UserListResponse mapToUserListResponse(User user) {
        return UserListResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .mfaEnabled(user.isMfaEnabled())
                .complaintCount(user.getComplaints() != null ? user.getComplaints().size() : 0)
                .build();
    }
} 