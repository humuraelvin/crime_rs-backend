package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.HotspotAnalysisRequest;
import com.crime.reporting.crime_reporting_backend.dto.HotspotResponse;
import com.crime.reporting.crime_reporting_backend.service.HotspotAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for crime hotspot analysis and visualization
 */
@RestController
@RequestMapping("/api/v1/hotspots")
@RequiredArgsConstructor
public class HotspotController {

    private final HotspotAnalysisService hotspotAnalysisService;

    /**
     * Analyze crime data to identify hotspots based on specified parameters
     * 
     * @param request Analysis parameters including time period and minimum cluster size
     * @return List of identified hotspots
     */
    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('POLICE', 'ADMIN')")
    public ResponseEntity<List<HotspotResponse>> analyzeHotspots(@Valid @RequestBody HotspotAnalysisRequest request) {
        List<HotspotResponse> hotspots = hotspotAnalysisService.identifyHotspots(
                request.startDate(), 
                request.endDate(), 
                request.minClusterSize()
        );
        
        return ResponseEntity.ok(hotspots);
    }
    
    /**
     * Get hotspots for a specific time range with default minimum cluster size
     * 
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @return List of identified hotspots
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('POLICE', 'ADMIN')")
    public ResponseEntity<List<HotspotResponse>> getHotspots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        // Default to clusters of at least 3 complaints
        int DEFAULT_MIN_CLUSTER_SIZE = 3;
        
        List<HotspotResponse> hotspots = hotspotAnalysisService.identifyHotspots(
                startDate, 
                endDate, 
                DEFAULT_MIN_CLUSTER_SIZE
        );
        
        return ResponseEntity.ok(hotspots);
    }
} 