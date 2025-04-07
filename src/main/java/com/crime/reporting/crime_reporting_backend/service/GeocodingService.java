package com.crime.reporting.crime_reporting_backend.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for handling geocoding operations using Google Maps API.
 */
@Service
public class GeocodingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    
    @Value("${google.maps.api-key}")
    private String apiKey;
    
    private GeoApiContext context;
    
    @PostConstruct
    public void init() {
        context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }
    
    /**
     * Geocodes an address string to latitude and longitude coordinates.
     * 
     * @param address The address to geocode
     * @return Coordinates object containing latitude and longitude, or null if geocoding fails
     */
    public Coordinates geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
            
            if (results.length > 0) {
                LatLng location = results[0].geometry.location;
                return new Coordinates(location.lat, location.lng);
            }
        } catch (Exception e) {
            logger.error("Error geocoding address: {}", address, e);
        }
        
        return null;
    }
    
    /**
     * Reverse geocodes coordinates to an address string.
     * 
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Address string, or null if reverse geocoding fails
     */
    public String reverseGeocode(double latitude, double longitude) {
        try {
            GeocodingResult[] results = GeocodingApi.reverseGeocode(context, new LatLng(latitude, longitude)).await();
            
            if (results.length > 0) {
                return results[0].formattedAddress;
            }
        } catch (Exception e) {
            logger.error("Error reverse geocoding coordinates: {}, {}", latitude, longitude, e);
        }
        
        return null;
    }
    
    /**
     * Simple record to hold latitude and longitude coordinates.
     */
    public record Coordinates(double latitude, double longitude) {}
} 