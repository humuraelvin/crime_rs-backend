package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.CrimeTypeCountDTO;
import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.DateCountDTO;
import com.crime.reporting.crime_reporting_backend.controller.ComplaintController.StatusCountDTO;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintRequest;
import com.crime.reporting.crime_reporting_backend.dto.ComplaintResponse;
import com.crime.reporting.crime_reporting_backend.dto.EvidenceResponse;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.entity.Evidence;
import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.CaseFileRepository;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import com.crime.reporting.crime_reporting_backend.repository.EvidenceRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.LazyInitializationException;
import org.hibernate.HibernateException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiPrioritizationService aiPrioritizationService;
    private final GeocodingService geocodingService;
    private final FileStorageService fileStorageService;

    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Complaint complaint = Complaint.builder()
                .user(user)
                .crimeType(request.getCrimeType())
                .description(request.getDescription())
                .status(ComplaintStatus.SUBMITTED)
                .location(request.getLocation())
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        
        // Calculate priority score using AI service
        int priorityScore = aiPrioritizationService.calculatePriorityScore(savedComplaint);
        savedComplaint.setPriorityScore(priorityScore);
        savedComplaint = complaintRepository.save(savedComplaint);
        
        return mapToComplaintResponse(savedComplaint);
    }
    
    @Transactional
    public ComplaintResponse createComplaintWithFiles(ComplaintRequest request, List<MultipartFile> files) {
        ComplaintResponse complaintResponse = createComplaint(request);
        if (files != null && !files.isEmpty()) {
            try {
                User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
                Complaint complaint = complaintRepository.findById(complaintResponse.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Complaint not found"));
                
                List<String> fileUrls = fileStorageService.storeFiles(files);
                
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    String fileUrl = fileUrls.get(i);
                    
                    // Determine evidence type based on content type
                    EvidenceType evidenceType = determineEvidenceType(file.getContentType());
                    
                    Evidence evidence = Evidence.builder()
                            .complaint(complaint)
                            .type(evidenceType)
                            .fileName(file.getOriginalFilename())
                            .fileUrl(fileUrl)
                            .fileContentType(file.getContentType())
                            .fileSize(file.getSize())
                            .uploadedBy(user)
                            .build();
                    
                    evidenceRepository.save(evidence);
                }
                
                // Get updated complaint with evidence
                return getComplaintById(complaintResponse.getId());
            } catch (IOException e) {
                throw new RuntimeException("Failed to store files", e);
            }
        }
        return complaintResponse;
    }
    
    private EvidenceType determineEvidenceType(String contentType) {
        if (contentType == null) {
            return EvidenceType.OTHER;
        }
        
        if (contentType.startsWith("image/")) {
            return EvidenceType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return EvidenceType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return EvidenceType.AUDIO;
        } else if (contentType.equals("application/pdf") || 
                  contentType.equals("application/msword") || 
                  contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                  contentType.equals("text/plain")) {
            return EvidenceType.DOCUMENT;
        } else {
            return EvidenceType.OTHER;
        }
    }

    @Cacheable(value = "complaints", key = "#id")
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findByIdWithEvidences(id)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found"));
        return mapToComplaintResponseSafely(complaint);
    }

    @Cacheable(value = "complaints", keyGenerator = "complexKeyGenerator")
    public Page<ComplaintResponse> getAllComplaints(
            ComplaintStatus status,
            CrimeType crimeType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        // Convert enums to strings to avoid PostgreSQL casting issues
        String statusStr = status != null ? status.name() : null;
        String crimeTypeStr = crimeType != null ? crimeType.name() : null;
        String startDateStr = startDate != null ? startDate.toString() : null;
        String endDateStr = endDate != null ? endDate.toString() : null;
        
        Page<Complaint> complaints = complaintRepository.findComplaintsWithFilters(
                statusStr, crimeTypeStr, startDateStr, endDateStr, pageable);
        
        // Safely map to response DTOs
        return complaints.map(complaint -> {
            try {
                return mapToComplaintResponseSafely(complaint);
            } catch (Exception e) {
                // Log the error but continue processing other complaints
                System.err.println("Error mapping complaint " + complaint.getId() + ": " + e.getMessage());
                return createSimpleComplaintResponse(complaint);
            }
        });
    }
    
    /**
     * Creates a simplified complaint response without relations that might cause LazyInitializationException
     */
    private ComplaintResponse createSimpleComplaintResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .userId(complaint.getUser().getId())
                .userName(complaint.getUser().getFirstName() + " " + complaint.getUser().getLastName())
                .crimeType(complaint.getCrimeType())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .dateFiled(complaint.getDateFiled())
                .dateLastUpdated(complaint.getDateLastUpdated())
                .location(complaint.getLocation())
                .priorityScore(complaint.getPriorityScore())
                // Skip potentially lazy-loaded collections
                .evidences(null)
                .build();
    }
    
    /**
     * Safely maps a complaint to a response DTO, handling potential LazyInitializationException
     */
    private ComplaintResponse mapToComplaintResponseSafely(Complaint complaint) {
        // Convert evidence entities to DTOs if present and accessible
        List<EvidenceResponse> evidenceResponses = null;
        
        try {
            if (complaint.getEvidences() != null) {
                // This might throw LazyInitializationException if the session is closed
                boolean hasEvidences = !complaint.getEvidences().isEmpty();
                
                if (hasEvidences) {
                    evidenceResponses = complaint.getEvidences().stream()
                            .map(evidence -> EvidenceResponse.builder()
                                    .id(evidence.getId())
                                    .complaintId(complaint.getId())
                                    .type(evidence.getType())
                                    .fileName(evidence.getFileName())
                                    .fileUrl(evidence.getFileUrl())
                                    .fileContentType(evidence.getFileContentType())
                                    .fileSize(evidence.getFileSize())
                                    .description(evidence.getDescription())
                                    .metadata(evidence.getMetadata())
                                    .uploadedAt(evidence.getUploadedAt())
                                    .uploadedById(evidence.getUploadedBy().getId())
                                    .uploadedByName(evidence.getUploadedBy().getFirstName() + " " + evidence.getUploadedBy().getLastName())
                                    .build())
                            .collect(Collectors.toList());
                }
            }
        } catch (HibernateException e) {
            // Log the error but continue with null evidences
            System.err.println("Error accessing evidences for complaint " + complaint.getId() + ": " + e.getMessage());
            evidenceResponses = null;
        }
        
        // Build and return the response
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .userId(complaint.getUser().getId())
                .userName(complaint.getUser().getFirstName() + " " + complaint.getUser().getLastName())
                .crimeType(complaint.getCrimeType())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .dateFiled(complaint.getDateFiled())
                .dateLastUpdated(complaint.getDateLastUpdated())
                .location(complaint.getLocation())
                .priorityScore(complaint.getPriorityScore())
                .evidences(evidenceResponses)
                .build();
    }

    @Cacheable(value = "userComplaints", key = "#userId")
    public Page<ComplaintResponse> getComplaintsByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<Complaint> complaints = complaintRepository.findByUser(user, pageable);
        return complaints.map(this::mapToComplaintResponse);
    }

    @Transactional
    @CacheEvict(value = {"complaints", "userComplaints"}, allEntries = true)
    public ComplaintResponse updateComplaintStatus(Long id, ComplaintStatus status) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found"));
        
        complaint.setStatus(status);
        complaint = complaintRepository.save(complaint);
        
        return mapToComplaintResponse(complaint);
    }
    
    @Transactional
    @CacheEvict(value = {"complaints", "userComplaints"}, allEntries = true)
    public void deleteComplaint(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found"));
        
        // Check if complaint has a case file
        boolean hasCaseFile = caseFileRepository.findByComplaint(complaint).isPresent();
        if (hasCaseFile) {
            throw new IllegalStateException("Cannot delete complaint that has an associated case file");
        }
        
        // Delete associated evidence first
        evidenceRepository.findByComplaint(complaint)
                .forEach(evidenceRepository::delete);
        
        // Now delete the complaint
        complaintRepository.delete(complaint);
    }
    
    @Cacheable(value = "complaintStats", key = "'byStatus'")
    public List<StatusCountDTO> getComplaintCountsByStatus() {
        List<StatusCountDTO> result = new ArrayList<>();
        for (ComplaintStatus status : ComplaintStatus.values()) {
            long count = complaintRepository.countByStatus(status);
            result.add(new StatusCountDTO(status, count));
        }
        return result;
    }
    
    @Cacheable(value = "complaintStats", key = "'byCrimeType'")
    public List<CrimeTypeCountDTO> getComplaintCountsByCrimeType() {
        List<Object[]> counts = complaintRepository.countByCrimeType();
        return counts.stream()
                .map(row -> new CrimeTypeCountDTO((CrimeType) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "complaintStats", key = "#startDate.toString() + #endDate.toString()")
    public List<DateCountDTO> getComplaintTrends(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> counts = complaintRepository.countByDateBetween(startDate, endDate);
        return counts.stream()
                .map(row -> new DateCountDTO((LocalDateTime) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    private ComplaintResponse mapToComplaintResponse(Complaint complaint) {
        // Convert evidence entities to DTOs if present
        List<EvidenceResponse> evidenceResponses = null;
        if (complaint.getEvidences() != null && !complaint.getEvidences().isEmpty()) {
            evidenceResponses = complaint.getEvidences().stream()
                    .map(evidence -> EvidenceResponse.builder()
                            .id(evidence.getId())
                            .complaintId(complaint.getId())
                            .type(evidence.getType())
                            .fileName(evidence.getFileName())
                            .fileUrl(evidence.getFileUrl())
                            .fileContentType(evidence.getFileContentType())
                            .fileSize(evidence.getFileSize())
                            .description(evidence.getDescription())
                            .metadata(evidence.getMetadata())
                            .uploadedAt(evidence.getUploadedAt())
                            .uploadedById(evidence.getUploadedBy().getId())
                            .uploadedByName(evidence.getUploadedBy().getFirstName() + " " + evidence.getUploadedBy().getLastName())
                            .build())
                    .collect(Collectors.toList());
        }
        
        // Build and return the response
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .userId(complaint.getUser().getId())
                .userName(complaint.getUser().getFirstName() + " " + complaint.getUser().getLastName())
                .crimeType(complaint.getCrimeType())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .dateFiled(complaint.getDateFiled())
                .dateLastUpdated(complaint.getDateLastUpdated())
                .location(complaint.getLocation())
                .priorityScore(complaint.getPriorityScore())
                .evidences(evidenceResponses)
                // CaseFile response would be added here if needed
                .build();
    }

    /**
     * Assigns a complaint to a police officer
     * @param complaintId the complaint ID
     * @param officerId the officer ID
     */
    public void assignComplaintToOfficer(Long complaintId, Long officerId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found with id: " + complaintId));
        
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new EntityNotFoundException("Police Officer not found with id: " + officerId));
        
        // Update complaint with officer and change status
        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setUpdatedAt(LocalDateTime.now());
        
        // Save updated complaint
        complaintRepository.save(complaint);
    }

    /**
     * Unassigns a complaint from its officer
     * @param complaintId the complaint ID
     */
    public void unassignComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found with id: " + complaintId));
        
        // Only unassign if currently assigned
        if (complaint.getAssignedOfficer() != null) {
            // Update complaint by removing officer
            complaint.setAssignedOfficer(null);
            
            // Set the status based on current status
            if (complaint.getStatus() == ComplaintStatus.ASSIGNED || 
                complaint.getStatus() == ComplaintStatus.INVESTIGATING || 
                complaint.getStatus() == ComplaintStatus.PENDING_EVIDENCE) {
                // If case is in active investigation, set it back to UNDER_REVIEW
                complaint.setStatus(ComplaintStatus.UNDER_REVIEW);
            }
            // For other statuses (like RESOLVED, REJECTED, CLOSED), keep the current status
            
            complaint.setUpdatedAt(LocalDateTime.now());
            
            // Save updated complaint
            complaintRepository.save(complaint);
        } else {
            throw new AccessDeniedException("Complaint is not currently assigned to any officer");
        }
    }

    /**
     * Gets complaints by status
     * @param status the status to filter by
     * @return list of complaints with the specified status
     */
    List<ComplaintDTO> getComplaintsByStatus(ComplaintStatus status);
} 