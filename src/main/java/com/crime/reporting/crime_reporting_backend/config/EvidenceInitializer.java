package com.crime.reporting.crime_reporting_backend.config;

import com.crime.reporting.crime_reporting_backend.entity.Evidence;
import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import com.crime.reporting.crime_reporting_backend.repository.EvidenceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EvidenceInitializer {
    
    private final EvidenceRepository evidenceRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Bean
    public CommandLineRunner initializeEvidence() {
        return args -> {
            fixNullEvidenceValues();
        };
    }
    
    @Transactional
    public void fixNullEvidenceValues() {
        log.info("Checking for evidence records with null values...");
        
        // Get all evidence records with null evidence_type or upload_date
        List<Evidence> evidencesToUpdate = entityManager.createQuery(
                "SELECT e FROM Evidence e WHERE e.evidenceType IS NULL OR e.uploadDate IS NULL", 
                Evidence.class)
                .getResultList();
        
        if (evidencesToUpdate.isEmpty()) {
            log.info("No evidence records with null values found.");
            return;
        }
        
        log.info("Found {} evidence records with null values. Updating...", evidencesToUpdate.size());
        
        // Update each evidence record
        for (Evidence evidence : evidencesToUpdate) {
            if (evidence.getEvidenceType() == null) {
                evidence.setEvidenceType(EvidenceType.OTHER);
                log.debug("Updated evidence ID: {} with evidence type: {}", evidence.getId(), EvidenceType.OTHER);
            }
            
            if (evidence.getUploadDate() == null) {
                // Use created_at if available, otherwise current time
                evidence.setUploadDate(LocalDateTime.now());
                log.debug("Updated evidence ID: {} with upload date: {}", evidence.getId(), evidence.getUploadDate());
            }
        }
        
        // Save all updated evidence records
        evidenceRepository.saveAll(evidencesToUpdate);
        
        log.info("Evidence records updated successfully!");
    }
} 