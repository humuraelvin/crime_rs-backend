package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.CrimeTypeCountDTO;
import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.DateCountDTO;
import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.StatusCountDTO;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintRequest;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintResponse;
import com.crime.reporting.crime_reporting_backend.dto.EvidenceResponse;
import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerStatisticsDTO;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.entity.Evidence;
import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.repository.CaseFileRepository;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import com.crime.reporting.crime_reporting_backend.repository.EvidenceRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import com.crime.reporting.crime_reporting_backend.repository.PoliceOfficerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.hibernate.HibernateException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service interface for handling complaints in the system
 */
public interface ComplaintService {
    
    /**
     * Creates a new complaint
     * @param request the complaint creation request
     * @return the created complaint
     */
    ComplaintDTO createComplaint(ComplaintRequest request);
    
    /**
     * Creates a new complaint with attached evidence files
     * @param request the complaint creation request
     * @param files the evidence files to attach
     * @return the created complaint
     */
    ComplaintDTO createComplaintWithFiles(ComplaintRequest request, List<MultipartFile> files) throws IOException;
    
    /**
     * Gets a complaint by its ID
     * @param id the complaint ID
     * @return the complaint data
     */
    ComplaintDTO getComplaintById(Long id);
    
    /**
     * Gets all complaints assigned to a specific police officer
     * @param policeOfficerId the ID of the police officer
     * @return list of complaints assigned to the police officer
     */
    List<ComplaintDTO> getComplaintsByPoliceOfficerId(Long policeOfficerId);
    
    /**
     * Gets statistics for a specific police officer
     * @param policeOfficerId the ID of the police officer
     * @return statistics about the officer's complaints
     */
    PoliceOfficerStatisticsDTO getPoliceOfficerStatistics(Long policeOfficerId);
    
    /**
     * Gets all complaints in the system
     * @return list of all complaints
     */
    List<ComplaintDTO> getAllComplaints();
    
    /**
     * Gets all complaints for admin with detailed information
     * @return list of all complaints with detailed information
     */
    List<ComplaintDTO> getAllComplaintsForAdmin();
    
    /**
     * Gets complaints by their status
     * @param status the status to filter by
     * @return list of complaints with the specified status
     */
    List<ComplaintDTO> getComplaintsByStatus(ComplaintStatus status);
    
    /**
     * Gets complaints submitted by a specific user
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return paged list of complaints submitted by the user
     */
    Page<ComplaintDTO> getComplaintsByUser(Long userId, Pageable pageable);
    
    /**
     * Assigns a complaint to a police officer
     * @param complaintId the ID of the complaint
     * @param officerId the ID of the police officer
     * @return the updated complaint
     */
    ComplaintDTO assignComplaintToOfficer(Long complaintId, Long officerId);
    
    /**
     * Unassigns a complaint from its current officer
     * @param complaintId the ID of the complaint
     * @return the updated complaint
     */
    ComplaintDTO unassignComplaint(Long complaintId);
    
    /**
     * Updates a complaint's status
     * @param complaintId the ID of the complaint
     * @param status the new status
     * @return the updated complaint
     */
    ComplaintDTO updateComplaintStatus(Long complaintId, ComplaintStatus status);
    
    /**
     * Updates a complaint's status with notes
     * @param complaintId the ID of the complaint
     * @param status the new status as string
     * @param notes optional notes about the status update
     * @return the updated complaint
     */
    ComplaintDTO updateComplaintStatus(Long complaintId, String status, String notes);
    
    /**
     * Gets count of complaints by status
     * @return list of status count objects
     */
    List<StatusCountDTO> getComplaintCountsByStatus();
    
    /**
     * Gets count of complaints by crime type
     * @return list of crime type count objects
     */
    List<CrimeTypeCountDTO> getComplaintCountsByCrimeType();
    
    /**
     * Gets complaint trends between two dates
     * @param startDate the start date
     * @param endDate the end date
     * @return list of date count objects
     */
    List<DateCountDTO> getComplaintTrends(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Deletes a complaint
     * @param id the ID of the complaint to delete
     */
    void deleteComplaint(Long id);

    /**
     * Updates a complaint's details (description, location, crimeType)
     *
     * @param complaintId the ID of the complaint to update
     * @param request the updated complaint data
     * @return the updated complaint
     */
    ComplaintDTO updateComplaintInfo(Long complaintId, ComplaintRequest request);
} 