package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.EvidenceRequest;
import com.crime.reporting.crime_reporting_backend.dto.EvidenceResponse;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.Evidence;
import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import com.crime.reporting.crime_reporting_backend.repository.EvidenceRepository;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public List<EvidenceResponse> uploadEvidenceFiles(List<MultipartFile> files, Long complaintId, Long userId) throws IOException {
        // Validate complaint and user exist
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found with ID: " + complaintId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        
        // Store files and get their URLs
        List<String> fileUrls = fileStorageService.storeFiles(files);
        
        List<Evidence> evidences = new ArrayList<>();
        
        // Create evidence records for each file
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
            
            evidences.add(evidenceRepository.save(evidence));
        }
        
        // Return evidence responses
        return evidences.stream()
                .map(this::mapToEvidenceResponse)
                .collect(Collectors.toList());
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
    
    private EvidenceResponse mapToEvidenceResponse(Evidence evidence) {
        return EvidenceResponse.builder()
                .id(evidence.getId())
                .complaintId(evidence.getComplaint().getId())
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
                .build();
    }
    
    @Transactional
    public void deleteEvidence(Long evidenceId) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new EntityNotFoundException("Evidence not found with ID: " + evidenceId));
        
        try {
            // Extract filename from fileUrl
            String fileUrl = evidence.getFileUrl();
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            
            // Delete the physical file
            fileStorageService.deleteFile(fileName);
            
            // Delete the database record
            evidenceRepository.delete(evidence);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete evidence file", e);
        }
    }
    
    public List<EvidenceResponse> getEvidenceByComplaintId(Long complaintId) {
        return evidenceRepository.findByComplaintId(complaintId).stream()
                .map(this::mapToEvidenceResponse)
                .collect(Collectors.toList());
    }
} 