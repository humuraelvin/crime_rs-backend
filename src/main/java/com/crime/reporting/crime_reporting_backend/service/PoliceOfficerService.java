package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerDTO;
import com.crime.reporting.crime_reporting_backend.entity.CaseStatus;
import com.crime.reporting.crime_reporting_backend.entity.Department;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.DepartmentRepository;
import com.crime.reporting.crime_reporting_backend.repository.PoliceOfficerRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PoliceOfficerService {

    private final PoliceOfficerRepository policeOfficerRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Gets all police officers
     * @return list of all police officers
     */
    public List<PoliceOfficerDTO> getAllOfficers() {
        return policeOfficerRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a police officer by ID
     * @param id the officer ID
     * @return the police officer details
     */
    public PoliceOfficerDTO getOfficerById(Long id) {
        PoliceOfficer officer = policeOfficerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Police officer not found with id: " + id));
        return mapToDTO(officer);
    }
    
    /**
     * Gets a police officer by user ID
     * @param userId the user ID
     * @return the police officer details
     */
    public PoliceOfficerDTO getOfficerByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        
        PoliceOfficer officer = policeOfficerRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Police officer not found for user id: " + userId));
        
        return mapToDTO(officer);
    }
    
    /**
     * Maps a PoliceOfficer entity to a DTO
     */
    private PoliceOfficerDTO mapToDTO(PoliceOfficer officer) {
        PoliceOfficerDTO dto = new PoliceOfficerDTO();
        dto.setId(officer.getId());
        
        // Set user-related information
        if (officer.getUser() != null) {
            User user = officer.getUser();
            dto.setUserId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
        }
        
        // Set officer-specific information
        dto.setBadgeNumber(officer.getBadgeNumber());
        dto.setRank(officer.getRank());
        dto.setSpecialization(officer.getSpecialization());
        dto.setContactInfo(officer.getContactInfo());
        dto.setJurisdiction(officer.getJurisdiction());
        
        // Set department information
        if (officer.getDepartment() != null) {
            dto.setDepartmentId(officer.getDepartment().getId());
            dto.setDepartmentName(officer.getDepartment().getName());
        }
        
        // Set active cases count
        if (officer.getCaseFiles() != null) {
            // Manually count active cases by checking which case files are not closed
            int activeCasesCount = (int) officer.getCaseFiles().stream()
                    .filter(caseFile -> caseFile.getStatus() != CaseStatus.CLOSED && 
                                       caseFile.getStatus() != CaseStatus.SUSPENDED)
                    .count();
            dto.setActiveCasesCount(activeCasesCount);
        } else {
            dto.setActiveCasesCount(0);
        }
        
        return dto;
    }
} 