package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.dto.AddressRequest;
import com.crime.reporting.crime_reporting_backend.dto.CoordinatesRequest;
import com.crime.reporting.crime_reporting_backend.dto.GeocodeResponse;
import com.crime.reporting.crime_reporting_backend.service.GeocodingService;
import com.crime.reporting.crime_reporting_backend.service.GeocodingService.Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Controller for handling location-based requests
 */
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final GeocodingService geocodingService;
    
    /**
     * Converts an address to coordinates
     * 
     * @param request The address to geocode
     * @return Geocoded coordinates
     */
    @PostMapping("/geocode")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'POLICE')")
    public ResponseEntity<GeocodeResponse> geocodeAddress(@Valid @RequestBody AddressRequest request) {
        Coordinates coordinates = geocodingService.geocodeAddress(request.address());
        
        if (coordinates == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(new GeocodeResponse(
            coordinates.latitude(),
            coordinates.longitude(),
            request.address()
        ));
    }
    
    /**
     * Converts coordinates to an address
     * 
     * @param request The coordinates to reverse geocode
     * @return Geocoded address
     */
    @PostMapping("/reverse-geocode")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'POLICE')")
    public ResponseEntity<GeocodeResponse> reverseGeocode(@Valid @RequestBody CoordinatesRequest request) {
        String address = geocodingService.reverseGeocode(request.latitude(), request.longitude());
        
        if (address == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(new GeocodeResponse(
            request.latitude(),
            request.longitude(),
            address
        ));
    }
} 