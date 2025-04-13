package com.crime.reporting.crime_reporting_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A generic API response object for consistent response structure
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
} 