package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.dto.HotspotResponse;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
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
        List<Complaint> complaints = complaintRepository.findByCreatedAtBetween(startDate, endDate);
        
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
            
            // Calculate distance between complaints
            double distance = calculateDistanceKm(
                centerComplaint.getLatitude(), centerComplaint.getLongitude(), 
                candidate.getLatitude(), candidate.getLongitude()
            );
            
            // If within cluster radius, add to cluster and recursively expand
            if (distance <= CLUSTER_RADIUS_KM) {
                cluster.add(candidate);
                processed.put(candidate.getId(), true);
                
                // Optional: recursive expansion for density-based clustering
                // expandCluster(candidate, allComplaints, cluster, processed);
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
        // Calculate centroid (average position) of the cluster
        double totalLat = 0;
        double totalLng = 0;
        
        for (Complaint complaint : cluster) {
            totalLat += complaint.getLatitude();
            totalLng += complaint.getLongitude();
        }
        
        double centerLat = totalLat / cluster.size();
        double centerLng = totalLng / cluster.size();
        
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
                centerLat,
                centerLng,
                cluster.size(),
                dominantCrimeType,
                crimeTypeCounts,
                avgSeverity,
                CLUSTER_RADIUS_KM
        );
    }
    
    /**
     * Calculates the distance between two points in kilometers using the Haversine formula
     * 
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        // Radius of the Earth in kilometers
        final double EARTH_RADIUS = 6371.0;
        
        // Convert coordinates from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        
        // Calculate differences
        double latDiff = lat2Rad - lat1Rad;
        double lonDiff = lon2Rad - lon1Rad;
        
        // Haversine formula
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
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