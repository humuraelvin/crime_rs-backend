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
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.CaseFileRepository;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import com.crime.reporting.crime_reporting_backend.repository.EvidenceRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // If location string is provided but no coordinates, try to geocode
        if (request.getLocation() != null && request.getLatitude() == null && request.getLongitude() == null) {
            var coordinates = geocodingService.geocodeAddress(request.getLocation());
            
            if (coordinates != null) {
                request.setLatitude(coordinates.latitude());
                request.setLongitude(coordinates.longitude());
            }
        }

        Complaint complaint = Complaint.builder()
                .user(user)
                .crimeType(request.getCrimeType())
                .description(request.getDescription())
                .status(ComplaintStatus.SUBMITTED)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(request.getLocation())
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        
        // Calculate priority score using AI service
        int priorityScore = aiPrioritizationService.calculatePriorityScore(savedComplaint);
        savedComplaint.setPriorityScore(priorityScore);
        savedComplaint = complaintRepository.save(savedComplaint);
        
        return mapToComplaintResponse(savedComplaint);
    }

    @Cacheable(value = "complaints", key = "#id")
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found"));
        return mapToComplaintResponse(complaint);
    }

    @Cacheable(value = "complaints", keyGenerator = "complexKeyGenerator")
    public Page<ComplaintResponse> getAllComplaints(
            ComplaintStatus status,
            CrimeType crimeType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<Complaint> complaints = complaintRepository.findComplaintsWithFilters(
                status, crimeType, startDate, endDate, pageable);
        return complaints.map(this::mapToComplaintResponse);
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
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .location(complaint.getLocation())
                .priorityScore(complaint.getPriorityScore())
                .evidences(evidenceResponses)
                // CaseFile response would be added here if needed
                .build();
    }
} 