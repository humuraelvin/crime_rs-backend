package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Service for AI-driven prioritization of complaints based on their severity.
 * In a production environment, this would be integrated with a real ML model,
 * but for demonstration purposes, we're using a rule-based approach.
 */
@Service
public class AiPrioritizationService {
    
    // Crime severity weights (1-100)
    private static final Map<CrimeType, Integer> CRIME_SEVERITY = new EnumMap<>(CrimeType.class);
    
    static {
        // Initialize crime severity weights
        CRIME_SEVERITY.put(CrimeType.HOMICIDE, 100);
        CRIME_SEVERITY.put(CrimeType.KIDNAPPING, 95);
        CRIME_SEVERITY.put(CrimeType.SEXUAL_ASSAULT, 90);
        CRIME_SEVERITY.put(CrimeType.DOMESTIC_VIOLENCE, 85);
        CRIME_SEVERITY.put(CrimeType.ROBBERY, 80);
        CRIME_SEVERITY.put(CrimeType.ASSAULT, 75);
        CRIME_SEVERITY.put(CrimeType.ARSON, 70);
        CRIME_SEVERITY.put(CrimeType.BURGLARY, 65);
        CRIME_SEVERITY.put(CrimeType.DRUG_RELATED, 60);
        CRIME_SEVERITY.put(CrimeType.THEFT, 55);
        CRIME_SEVERITY.put(CrimeType.FRAUD, 50);
        CRIME_SEVERITY.put(CrimeType.CYBER_CRIME, 45);
        CRIME_SEVERITY.put(CrimeType.VANDALISM, 40);
        CRIME_SEVERITY.put(CrimeType.HARASSMENT, 35);
        CRIME_SEVERITY.put(CrimeType.OTHER, 30);
    }
    
    /**
     * Calculates a priority score for a given complaint.
     * The score is based on several factors including:
     * - Crime type severity
     * - Keywords in the description
     * - Geographical factors
     * 
     * @param complaint The complaint to score
     * @return Priority score (0-100), higher means more urgent
     */
    public int calculatePriorityScore(Complaint complaint) {
        int score = 0;
        
        // Base score from crime type (0-70 points)
        score += getBaseCrimeTypeScore(complaint.getCrimeType());
        
        // Additional points from description analysis (0-15 points)
        score += analyzeDescription(complaint.getDescription());
        
        // Additional points for geographic factors (0-10 points)
        score += analyzeGeographicFactors(complaint.getLatitude(), complaint.getLongitude());
        
        // Additional points for recency factors (0-5 points)
        // Newer complaints might get slightly higher priority
        score += 5; // All new complaints get this bonus
        
        // Ensure the score is within 0-100 range
        return Math.min(100, Math.max(0, score));
    }
    
    private int getBaseCrimeTypeScore(CrimeType crimeType) {
        // Get the base severity and scale it to 0-70 range
        int baseSeverity = CRIME_SEVERITY.getOrDefault(crimeType, 30);
        return (int) (baseSeverity * 0.7); // Scale to max 70 points
    }
    
    private int analyzeDescription(String description) {
        if (description == null || description.isEmpty()) {
            return 0;
        }
        
        String lowerDesc = description.toLowerCase();
        int score = 0;
        
        // Check for urgent keywords
        if (containsAny(lowerDesc, "weapon", "gun", "knife", "armed", "blood", "bleeding", "injured", "hurt badly")) {
            score += 7;
        }
        
        // Check for danger indicators
        if (containsAny(lowerDesc, "threat", "threatening", "threatened", "danger", "dangerous", "scared", "afraid", "fear")) {
            score += 5;
        }
        
        // Check for vulnerable victims
        if (containsAny(lowerDesc, "child", "children", "kid", "baby", "elderly", "disabled", "pregnant")) {
            score += 5;
        }
        
        // Check for crime in progress
        if (containsAny(lowerDesc, "happening now", "in progress", "right now", "currently", "ongoing")) {
            score += 10;
        }
        
        return Math.min(15, score); // Cap at 15 points
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private int analyzeGeographicFactors(Double latitude, Double longitude) {
        // In a real system, this would check crime hotspots, proximity to schools, etc.
        // For demonstration, we'll just return a value if coordinates are provided
        if (latitude != null && longitude != null) {
            return 5; // We have location data, which helps prioritization
        }
        return 0;
    }
} 