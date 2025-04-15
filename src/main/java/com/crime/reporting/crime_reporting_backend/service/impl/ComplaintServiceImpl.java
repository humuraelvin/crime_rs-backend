package com.crime.reporting.crime_reporting_backend.service.impl;

import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.CrimeTypeCountDTO;
import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.DateCountDTO;
import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.StatusCountDTO;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintDTO;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintRequest;
import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerStatisticsDTO;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.entity.Evidence;
import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.exception.ResourceNotFoundException;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import com.crime.reporting.crime_reporting_backend.repository.EvidenceRepository;
import com.crime.reporting.crime_reporting_backend.repository.PoliceOfficerRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import com.crime.reporting.crime_reporting_backend.service.ComplaintService;
import com.crime.reporting.crime_reporting_backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ComplaintDTO createComplaint(ComplaintRequest request) {
        log.info("Creating new complaint: {}", request);
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
        
        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setCrimeType(request.getCrimeType());
        complaint.setDescription(request.getDescription());
        complaint.setLocation(request.getLocation());
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setDateFiled(LocalDateTime.now());
        complaint.setDateLastUpdated(LocalDateTime.now());
        
        Complaint savedComplaint = complaintRepository.save(complaint);
        log.info("Created complaint with ID: {}", savedComplaint.getId());
        
        return mapToDTO(savedComplaint);
    }
    
    @Override
    @Transactional
    public ComplaintDTO createComplaintWithFiles(ComplaintRequest request, List<MultipartFile> files) throws IOException {
        log.info("Creating new complaint with {} files: {}", files.size(), request);
        
        ComplaintDTO complaintDTO = createComplaint(request);
        Complaint complaint = complaintRepository.findById(complaintDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintDTO.getId()));
        
        List<Evidence> evidences = new ArrayList<>();
        
        for (MultipartFile file : files) {
            String fileName = fileStorageService.storeFile(file);
            
            Evidence evidence = new Evidence();
            evidence.setComplaint(complaint);
            evidence.setFileName(fileName);
            evidence.setOriginalFileName(file.getOriginalFilename());
            evidence.setFileType(file.getContentType());
            evidence.setFileSize(file.getSize());
            evidence.setEvidenceType(determineEvidenceType(file.getContentType()));
            evidence.setUploadDate(LocalDateTime.now());
            
            evidences.add(evidence);
        }
        
        if (!evidences.isEmpty()) {
            evidenceRepository.saveAll(evidences);
            complaint.setEvidences(evidences);
            complaintRepository.save(complaint);
            
            log.info("Added {} evidence files to complaint ID: {}", evidences.size(), complaint.getId());
        }
        
        return mapToDTO(complaint);
    }
    
    private EvidenceType determineEvidenceType(String contentType) {
        if (contentType == null) return EvidenceType.OTHER;
        
        if (contentType.startsWith("image/")) {
            return EvidenceType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return EvidenceType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return EvidenceType.AUDIO;
        } else if (contentType.equals("application/pdf") || 
                  contentType.contains("document") || 
                  contentType.contains("spreadsheet") || 
                  contentType.contains("presentation")) {
            return EvidenceType.DOCUMENT;
        } else {
            return EvidenceType.OTHER;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ComplaintDTO getComplaintById(Long id) {
        log.info("Fetching complaint with ID: {}", id);
        
        Complaint complaint = complaintRepository.findByIdWithEvidences(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
        
        return mapToDTO(complaint);
    }

    @Override
    @Transactional
    public List<ComplaintDTO> getComplaintsByPoliceOfficerId(Long policeOfficerId) {
        log.info("Fetching complaints for police officer with ID: {}", policeOfficerId);
        
        // Check if the police officer exists
        if (!policeOfficerRepository.existsById(policeOfficerId)) {
            log.error("Police officer with ID {} not found", policeOfficerId);
            throw new ResourceNotFoundException("Police officer not found with ID: " + policeOfficerId);
        }
        
        List<Complaint> complaints = complaintRepository.findByAssignedOfficerId(policeOfficerId);
        log.info("Found {} complaints assigned to police officer ID: {}", complaints.size(), policeOfficerId);
        
        return complaints.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public PoliceOfficerStatisticsDTO getPoliceOfficerStatistics(Long policeOfficerId) {
        log.info("Generating statistics for police officer with ID: {}", policeOfficerId);
        
        // Check if the police officer exists
        PoliceOfficer officer = policeOfficerRepository.findById(policeOfficerId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with ID: " + policeOfficerId));
        
        List<Complaint> officerComplaints = complaintRepository.findByAssignedOfficerId(policeOfficerId);
        
        // Count complaints by status
        Map<ComplaintStatus, Long> complaintsByStatus = officerComplaints.stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));
        
        // Get counts for specific statuses or 0
        long submittedCount = complaintsByStatus.getOrDefault(ComplaintStatus.SUBMITTED, 0L);
        long assignedCount = complaintsByStatus.getOrDefault(ComplaintStatus.ASSIGNED, 0L);
        long investigating = complaintsByStatus.getOrDefault(ComplaintStatus.INVESTIGATING, 0L);
        long resolved = complaintsByStatus.getOrDefault(ComplaintStatus.RESOLVED, 0L);
        long closed = complaintsByStatus.getOrDefault(ComplaintStatus.CLOSED, 0L);
        
        return PoliceOfficerStatisticsDTO.builder()
                .officerId(policeOfficerId)
                .officerName(officer.getUser().getFirstName() + " " + officer.getUser().getLastName())
                .totalAssignedComplaints(officerComplaints.size())
                .pendingComplaints((int) (submittedCount + assignedCount))
                .inProgressComplaints((int) investigating)
                .resolvedComplaints((int) resolved)
                .closedComplaints((int) closed)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getAllComplaints() {
        log.info("Fetching all complaints");
        List<Complaint> complaints = complaintRepository.findAll();
        return complaints.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getAllComplaintsForAdmin() {
        log.info("Fetching all complaints with details for admin");
        List<Complaint> complaints = complaintRepository.findAll();
        return complaints.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getComplaintsByStatus(ComplaintStatus status) {
        log.info("Fetching complaints with status: {}", status);
        List<Complaint> complaints = complaintRepository.findByStatus(status);
        return complaints.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintDTO> getComplaintsByUser(Long userId, Pageable pageable) {
        log.info("Fetching complaints for user with ID: {}, page: {}", userId, pageable);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        Page<Complaint> complaintsPage = complaintRepository.findByUser(user, pageable);
        List<ComplaintDTO> complaintDTOs = complaintsPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(complaintDTOs, pageable, complaintsPage.getTotalElements());
    }
    
    @Override
    @Transactional
    public ComplaintDTO assignComplaintToOfficer(Long complaintId, Long officerId) {
        log.info("Assigning complaint {} to officer {}", complaintId, officerId);
        
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
                
        PoliceOfficer officer = policeOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found with id: " + officerId));
        
        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setDateLastUpdated(LocalDateTime.now());
        
        Complaint updatedComplaint = complaintRepository.save(complaint);
        log.info("Successfully assigned complaint {} to officer {}. Assigned officer ID in DB: {}", 
                complaintId, officerId, updatedComplaint.getAssignedOfficer() != null ? updatedComplaint.getAssignedOfficer().getId() : "null");
        
        return mapToDTO(updatedComplaint);
    }
    
    @Override
    @Transactional
    public ComplaintDTO unassignComplaint(Long complaintId) {
        log.info("Unassigning complaint {}", complaintId);
        
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        
        complaint.setAssignedOfficer(null);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setDateLastUpdated(LocalDateTime.now());
        
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return mapToDTO(updatedComplaint);
    }
    
    @Override
    @Transactional
    public ComplaintDTO updateComplaintStatus(Long complaintId, ComplaintStatus status) {
        log.info("Updating complaint {} status to {}", complaintId, status);
        
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        
        complaint.setStatus(status);
        complaint.setDateLastUpdated(LocalDateTime.now());
        
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return mapToDTO(updatedComplaint);
    }
    
    /**
     * Updates the status of a complaint with additional notes
     * @param complaintId the ID of the complaint to update
     * @param status the new status as a string
     * @param notes optional notes about the status update
     * @return the updated complaint data
     */
    @Transactional
    public ComplaintDTO updateComplaintStatus(Long complaintId, String status, String notes) {
        log.info("Updating complaint {} status to {} with notes", complaintId, status);
        
        ComplaintStatus complaintStatus;
        try {
            complaintStatus = ComplaintStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        
        complaint.setStatus(complaintStatus);
        complaint.setDateLastUpdated(LocalDateTime.now());
        
        // If notes are provided, add them as a comment
        if (notes != null && !notes.trim().isEmpty()) {
            // Add implementation to save notes as a comment if you have a comments entity
            // For now, you can add to description or a separate notes field if available
            // Example: complaint.setNotes(notes);
        }
        
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return mapToDTO(updatedComplaint);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<StatusCountDTO> getComplaintCountsByStatus() {
        log.info("Getting complaint counts by status");
        
        List<StatusCountDTO> result = new ArrayList<>();
        for (ComplaintStatus status : ComplaintStatus.values()) {
            long count = complaintRepository.countByStatus(status);
            result.add(new StatusCountDTO(status.name(), count));
        }
        
        return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CrimeTypeCountDTO> getComplaintCountsByCrimeType() {
        log.info("Getting complaint counts by crime type");
        
        List<Object[]> results = complaintRepository.countByCrimeType();
        List<CrimeTypeCountDTO> counts = new ArrayList<>();
        
        for (Object[] result : results) {
            CrimeType crimeType = (CrimeType) result[0];
            Long count = (Long) result[1];
            counts.add(new CrimeTypeCountDTO(crimeType.name(), count));
        }
        
        return counts;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<DateCountDTO> getComplaintTrends(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting complaint trends between {} and {}", startDate, endDate);
        
        List<Object[]> results = complaintRepository.countByDateBetween(startDate, endDate);
        List<DateCountDTO> trends = new ArrayList<>();
        
        for (Object[] result : results) {
            String date = result[0].toString();
            Long count = (Long) result[1];
            trends.add(new DateCountDTO(date, count));
        }
        
        return trends;
    }
    
    @Override
    @Transactional
    public void deleteComplaint(Long id) {
        log.info("Deleting complaint with ID: {}", id);
        
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
        
        // Delete associated evidence files
        if (complaint.getEvidences() != null && !complaint.getEvidences().isEmpty()) {
            for (Evidence evidence : complaint.getEvidences()) {
                try {
                    fileStorageService.deleteFile(evidence.getFileName());
                } catch (Exception e) {
                    log.error("Error deleting evidence file {}: {}", evidence.getFileName(), e.getMessage());
                }
            }
            evidenceRepository.deleteAll(complaint.getEvidences());
        }
        
        complaintRepository.delete(complaint);
    }
    
    private ComplaintDTO mapToDTO(Complaint complaint) {
        return ComplaintDTO.builder()
                .id(complaint.getId())
                .title(complaint.getDescription())  // Using description as title if title doesn't exist
                .description(complaint.getDescription())
                .location(complaint.getLocation())
                .incidentDate(complaint.getDateFiled().toString())  // Using dateFiled as incidentDate if incidentDate doesn't exist
                .status(complaint.getStatus())
                .category(complaint.getCrimeType())
                .userId(complaint.getUser() != null ? complaint.getUser().getId() : null)
                .userName(complaint.getUser() != null ? 
                    complaint.getUser().getFirstName() + " " + complaint.getUser().getLastName() : null)
                .userContact(complaint.getUser() != null ? complaint.getUser().getEmail() : null)
                .assignedOfficerId(complaint.getAssignedOfficer() != null ? complaint.getAssignedOfficer().getId() : null)
                .assignedOfficerName(complaint.getAssignedOfficer() != null ? 
                    complaint.getAssignedOfficer().getUser().getFirstName() + " " + 
                    complaint.getAssignedOfficer().getUser().getLastName() : null)
                .evidenceFileNames(complaint.getEvidences() != null ? 
                    complaint.getEvidences().stream().map(Evidence::getFileName).collect(Collectors.toList()) : null)
                .createdAt(complaint.getDateFiled())
                .updatedAt(complaint.getDateLastUpdated())
                .build();
    }
} 