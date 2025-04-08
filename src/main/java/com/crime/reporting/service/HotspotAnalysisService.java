package com.crime.reporting.service;

import com.crime.reporting.model.Complaint;
import com.crime.reporting.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotspotAnalysisService {
    private final ComplaintRepository complaintRepository;

    public Map<String, Long> getCrimeHotspots() {
        List<Complaint> complaints = complaintRepository.findAll();
        
        return complaints.stream()
                .collect(Collectors.groupingBy(
                        Complaint::getLocation,
                        Collectors.counting()
                ));
    }
} 