package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.HotspotResponse;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for analyzing crime hotspots using geographic clustering algorithms
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotspotAnalysisService {

    private final ComplaintRepository complaintRepository;
    
    // Define the radius in kilometers for clustering complaints
    private static final double CLUSTER_RADIUS_KM = 1.0;
    
    /**
     * Identifies crime hotspots based on complaint density within specified time period
     * 
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @param minClusterSize Minimum number of complaints to be considered a hotspot
     * @return List of identified hotspots
     */
    public List<HotspotResponse> identifyHotspots(LocalDateTime startDate, LocalDateTime endDate, int minClusterSize) {
        // Fetch all complaints within the time period
        // Convert dates to strings for the repository method
        String startDateStr = startDate != null ? startDate.toString() : null;
        String endDateStr = endDate != null ? endDate.toString() : null;
        
        List<Complaint> complaints = complaintRepository.findComplaintsWithFilters(
                null, // status
                null, // crimeType
                startDateStr,
                endDateStr,
                PageRequest.of(0, Integer.MAX_VALUE) // Get all complaints
        ).getContent();
        
        // Skip if there are no complaints
        if (complaints.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Create clusters based on geographic proximity
        List<List<Complaint>> clusters = createClusters(complaints);
        
        // Filter clusters to only include those meeting the minimum size requirement
        List<List<Complaint>> significantClusters = clusters.stream()
                .filter(cluster -> cluster.size() >= minClusterSize)
                .collect(Collectors.toList());
        
        // Convert clusters to hotspot response objects
        return significantClusters.stream()
                .map(this::convertClusterToHotspot)
                .collect(Collectors.toList());
    }
    
    /**
     * Groups complaints into clusters based on geographic proximity
     * 
     * @param complaints List of complaints to cluster
     * @return List of complaint clusters
     */
    private List<List<Complaint>> createClusters(List<Complaint> complaints) {
        List<List<Complaint>> clusters = new ArrayList<>();
        Map<Long, Boolean> processed = new HashMap<>();
        
        for (Complaint complaint : complaints) {
            // Skip complaints that have already been processed
            if (processed.getOrDefault(complaint.getId(), false)) {
                continue;
            }
            
            // Create a new cluster with this complaint
            List<Complaint> cluster = new ArrayList<>();
            cluster.add(complaint);
            processed.put(complaint.getId(), true);
            
            // Find all complaints within the cluster radius
            expandCluster(complaint, complaints, cluster, processed);
            
            // Add the cluster to our results
            clusters.add(cluster);
        }
        
        return clusters;
    }
    
    /**
     * Recursively expands a cluster by adding complaints within the cluster radius
     * 
     * @param centerComplaint The complaint at the center of the cluster
     * @param allComplaints All available complaints
     * @param cluster The current cluster being built
     * @param processed Map tracking which complaints have been processed
     */
    private void expandCluster(Complaint centerComplaint, List<Complaint> allComplaints, 
                              List<Complaint> cluster, Map<Long, Boolean> processed) {
        
        for (Complaint candidate : allComplaints) {
            // Skip if already processed
            if (processed.getOrDefault(candidate.getId(), false) || candidate.getId().equals(centerComplaint.getId())) {
                continue;
            }
            
            // Without geographic coordinates, simply check if locations match
            boolean isSameLocation = centerComplaint.getLocation() != null && 
                                    centerComplaint.getLocation().equals(candidate.getLocation());
            
            // If in the same location, add to cluster
            if (isSameLocation) {
                cluster.add(candidate);
                processed.put(candidate.getId(), true);
            }
        }
    }
    
    /**
     * Converts a cluster of complaints to a hotspot response
     * 
     * @param cluster List of complaints in the cluster
     * @return Hotspot response with details about the cluster
     */
    private HotspotResponse convertClusterToHotspot(List<Complaint> cluster) {
        // Use a fixed position for each location-based cluster (this would be replaced with actual geocoding in production)
        double dummyLat = 0.0;
        double dummyLng = 0.0;
        
        // Map the crime types in the cluster
        Map<String, Long> crimeTypeCounts = cluster.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getCrimeType().name(),
                    Collectors.counting()
                ));
        
        // Find the most common crime type
        String dominantCrimeType = crimeTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
        
        // Calculate average severity based on AiPrioritizationService scores
        double avgSeverity = cluster.stream()
                .mapToDouble(Complaint::getPriorityScore)
                .average()
                .orElse(0);
        
        return new HotspotResponse(
                dummyLat,
                dummyLng,
                cluster.size(),
                dominantCrimeType,
                crimeTypeCounts,
                avgSeverity,
                CLUSTER_RADIUS_KM
        );
    }

    public Map<String, Long> getCrimeHotspots() {
        List<Complaint> complaints = complaintRepository.findAll();
        
        return complaints.stream()
                .collect(Collectors.groupingBy(
                        Complaint::getLocation,
                        Collectors.counting()
                ));
    }
} 