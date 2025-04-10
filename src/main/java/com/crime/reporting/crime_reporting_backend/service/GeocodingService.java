package com.crime.reporting.crime_reporting_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Simple service for handling coordinates. Does not use external APIs.
 */
@Service
public class GeocodingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    
    /**
     * A placeholder method that would normally geocode an address.
     * Since we're not using Google Maps API, this simply returns null.
     * 
     * @param address The address string
     * @return null (no geocoding without API)
     */
    public Coordinates geocodeAddress(String address) {
        logger.info("Geocoding is disabled. Address: {}", address);
        return null;
    }
    
    /**
     * A placeholder method that would normally reverse geocode coordinates.
     * Since we're not using Google Maps API, this simply returns the coordinates as a string.
     * 
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return A string representation of the coordinates
     */
    public String reverseGeocode(double latitude, double longitude) {
        String coordString = String.format("Lat: %.6f, Lng: %.6f", latitude, longitude);
        logger.info("Reverse geocoding is disabled. Returning coordinate string: {}", coordString);
        return coordString;
    }
    
    /**
     * Simple record to hold latitude and longitude coordinates.
     */
    public record Coordinates(double latitude, double longitude) {}
} 