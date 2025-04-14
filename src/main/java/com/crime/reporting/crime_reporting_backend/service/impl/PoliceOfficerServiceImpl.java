package com.crime.reporting.crime_reporting_backend.service.impl;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerDTO;
import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerResponse;
import com.crime.reporting.crime_reporting_backend.entity.Department;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.exception.ResourceNotFoundException;
import com.crime.reporting.crime_reporting_backend.repository.DepartmentRepository;
import com.crime.reporting.crime_reporting_backend.repository.PoliceOfficerRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import com.crime.reporting.crime_reporting_backend.service.PoliceOfficerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PoliceOfficerServiceImpl implements PoliceOfficerService {

    private final PoliceOfficerRepository policeOfficerRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public List<PoliceOfficerResponse> getAllPoliceOfficers() {
        log.info("Fetching all police officers");
        List<PoliceOfficer> officers = policeOfficerRepository.findAll();
        return officers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PoliceOfficerResponse getPoliceOfficerById(Long id) {
        log.info("Fetching police officer with id: {}", id);
        PoliceOfficer policeOfficer = policeOfficerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + id));
        return mapToResponse(policeOfficer);
    }

    @Override
    public PoliceOfficerDTO getOfficerByUserId(Long userId) {
        log.info("Fetching police officer with user id: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            
            PoliceOfficer officer = policeOfficerRepository.findByUser(user)
                    .orElseThrow(() -> new ResourceNotFoundException("Police officer not found for user id: " + userId));
            
            Department department = officer.getDepartment();
            
            PoliceOfficerDTO dto = new PoliceOfficerDTO();
            dto.setId(officer.getId());
            dto.setUserId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setBadgeNumber(officer.getBadgeNumber());
            dto.setRank(officer.getRank());
            dto.setSpecialization(officer.getSpecialization());
            dto.setDepartmentId(department != null ? department.getId() : null);
            dto.setDepartmentName(department != null ? department.getName() : null);
            dto.setContactInfo(officer.getContactInfo());
            dto.setJurisdiction(officer.getJurisdiction());
            dto.setCreatedAt(officer.getCreatedAt());
            dto.setUpdatedAt(officer.getUpdatedAt());
            
            return dto;
        } catch (Exception e) {
            log.error("Error fetching police officer with user id: {}", userId, e);
            throw new ResourceNotFoundException("Failed to retrieve police officer details");
        }
    }

    @Override
    public boolean existsByBadgeNumber(String badgeNumber) {
        log.info("Checking if police officer exists with badge number: {}", badgeNumber);
        return policeOfficerRepository.existsByBadgeNumber(badgeNumber);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        log.info("Checking if police officer exists with user id: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        return policeOfficerRepository.existsByUser(userOpt.get());
    }

    @Override
    public void updatePoliceOfficer(Long id, PoliceOfficerDTO officerDTO) {
        log.info("Updating police officer with id: {}", id);
        PoliceOfficer officer = policeOfficerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + id));
        
        // Update officer fields
        if (officerDTO.getBadgeNumber() != null) {
            officer.setBadgeNumber(officerDTO.getBadgeNumber());
        }
        if (officerDTO.getRank() != null) {
            officer.setRank(officerDTO.getRank());
        }
        if (officerDTO.getSpecialization() != null) {
            officer.setSpecialization(officerDTO.getSpecialization());
        }
        if (officerDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(officerDTO.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + officerDTO.getDepartmentId()));
            officer.setDepartment(department);
        }
        if (officerDTO.getContactInfo() != null) {
            officer.setContactInfo(officerDTO.getContactInfo());
        }
        if (officerDTO.getJurisdiction() != null) {
            officer.setJurisdiction(officerDTO.getJurisdiction());
        }
        
        policeOfficerRepository.save(officer);
    }
    
    // Helper method to map PoliceOfficer to PoliceOfficerResponse
    private PoliceOfficerResponse mapToResponse(PoliceOfficer officer) {
        PoliceOfficerResponse response = new PoliceOfficerResponse();
        response.setId(officer.getId());
        
        if (officer.getUser() != null) {
            User user = officer.getUser();
            response.setUserId(user.getId());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setEmail(user.getEmail());
            response.setPhoneNumber(user.getPhoneNumber());
        }
        
        response.setBadgeNumber(officer.getBadgeNumber());
        response.setRank(officer.getRank());
        response.setSpecialization(officer.getSpecialization());
        
        if (officer.getDepartment() != null) {
            response.setDepartmentId(officer.getDepartment().getId());
            response.setDepartmentName(officer.getDepartment().getName());
        }
        
        response.setContactInfo(officer.getContactInfo());
        response.setJurisdiction(officer.getJurisdiction());
        response.setCreatedAt(officer.getCreatedAt());
        response.setUpdatedAt(officer.getUpdatedAt());
        
        return response;
    }
} 