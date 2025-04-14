package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerDTO;
import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerResponse;

import java.util.List;

/**
 * Service interface for managing police officers
 */
public interface PoliceOfficerService {
    
    /**
     * Gets all police officers
     * @return list of all police officers
     */
    List<PoliceOfficerResponse> getAllPoliceOfficers();
    
    /**
     * Gets a police officer by ID
     * @param id the officer ID
     * @return the police officer details
     */
    PoliceOfficerResponse getPoliceOfficerById(Long id);
    
    /**
     * Gets a police officer by user ID
     * @param userId the user ID
     * @return the police officer details
     */
    PoliceOfficerDTO getOfficerByUserId(Long userId);
    
    /**
     * Checks if a police officer exists with the given badge number
     * @param badgeNumber the badge number
     * @return true if exists, false otherwise
     */
    boolean existsByBadgeNumber(String badgeNumber);
    
    /**
     * Checks if a police officer exists with the given user ID
     * @param userId the user ID
     * @return true if exists, false otherwise
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Updates a police officer
     * @param id the officer ID
     * @param officerDTO the updated details
     */
    void updatePoliceOfficer(Long id, PoliceOfficerDTO officerDTO);
} 