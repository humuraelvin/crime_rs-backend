package com.crime.reporting.crime_reporting_backend.controller;

import com.crime.reporting.crime_reporting_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    @GetMapping("/test-access")
    public ResponseEntity<Map<String, Object>> testAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        log.info("Test access endpoint called by: {}", authentication.getName());
        log.info("User has authorities: {}", authentication.getAuthorities());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You have access to the reports API!");
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities().toString());
        response.put("isAuthenticated", authentication.isAuthenticated());
        response.put("principal", authentication.getPrincipal().toString());
        response.put("details", authentication.getDetails() != null ? authentication.getDetails().toString() : "null");
        
        // Check if user has required roles
        boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean hasPoliceOfficerRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_POLICE_OFFICER"));
        
        response.put("hasAdminRole", hasAdminRole);
        response.put("hasPoliceOfficerRole", hasPoliceOfficerRole);
        response.put("hasRequiredRoles", hasAdminRole || hasPoliceOfficerRole);
        
        return ResponseEntity.ok(response);
    }

    // Support both GET and POST for complaints report
    @GetMapping("/complaints")
    public ResponseEntity<InputStreamResource> getComplaintsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String crimeType) {
        
        log.info("Generating complaints report (GET): startDate={}, endDate={}, status={}, crimeType={}",
                startDate, endDate, status, crimeType);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Request by user: {} with roles: {}", auth.getName(), auth.getAuthorities());
        
        return generateComplaintsReportResponse(startDate, endDate, status, crimeType);
    }
    
    @PostMapping("/complaints")
    public ResponseEntity<InputStreamResource> postComplaintsReport(
            @RequestBody(required = false) Map<String, Object> params) {
        
        LocalDateTime startDate = getDateTimeParam(params, "startDate");
        LocalDateTime endDate = getDateTimeParam(params, "endDate");
        String status = getStringParam(params, "status");
        String crimeType = getStringParam(params, "crimeType");
        
        log.info("Generating complaints report (POST): startDate={}, endDate={}, status={}, crimeType={}",
                startDate, endDate, status, crimeType);
        
        return generateComplaintsReportResponse(startDate, endDate, status, crimeType);
    }
    
    private ResponseEntity<InputStreamResource> generateComplaintsReportResponse(
            LocalDateTime startDate, LocalDateTime endDate, String status, String crimeType) {
        
        ByteArrayInputStream bis = reportService.generateComplaintsReport(startDate, endDate, status, crimeType);
        
        String filename = "Complaints_Report_" + LocalDateTime.now().format(DATE_FORMATTER) + ".pdf";
        return generateResponseFromInputStream(bis, filename);
    }
    
    // Support both GET and POST for officers performance report
    @GetMapping("/officers-performance")
    public ResponseEntity<InputStreamResource> getOfficersPerformanceReport(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Generating officers performance report (GET): departmentId={}, startDate={}, endDate={}",
                departmentId, startDate, endDate);
        
        return generateOfficersPerformanceReportResponse(departmentId, startDate, endDate);
    }
    
    @PostMapping("/officers-performance")
    public ResponseEntity<InputStreamResource> postOfficersPerformanceReport(
            @RequestBody(required = false) Map<String, Object> params) {
        
        Long departmentId = getLongParam(params, "departmentId");
        LocalDateTime startDate = getDateTimeParam(params, "startDate");
        LocalDateTime endDate = getDateTimeParam(params, "endDate");
        
        log.info("Generating officers performance report (POST): departmentId={}, startDate={}, endDate={}",
                departmentId, startDate, endDate);
        
        return generateOfficersPerformanceReportResponse(departmentId, startDate, endDate);
    }
    
    private ResponseEntity<InputStreamResource> generateOfficersPerformanceReportResponse(
            Long departmentId, LocalDateTime startDate, LocalDateTime endDate) {
        
        ByteArrayInputStream bis = reportService.generateOfficersPerformanceReport(departmentId, startDate, endDate);
        
        String filename = "Officers_Performance_Report_" + LocalDateTime.now().format(DATE_FORMATTER) + ".pdf";
        return generateResponseFromInputStream(bis, filename);
    }
    
    // Support both GET and POST for user activity report
    @GetMapping("/user-activity")
    public ResponseEntity<InputStreamResource> getUserActivityReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Generating user activity report (GET): startDate={}, endDate={}", startDate, endDate);
        
        return generateUserActivityReportResponse(startDate, endDate);
    }
    
    @PostMapping("/user-activity")
    public ResponseEntity<InputStreamResource> postUserActivityReport(
            @RequestBody(required = false) Map<String, Object> params) {
        
        LocalDateTime startDate = getDateTimeParam(params, "startDate");
        LocalDateTime endDate = getDateTimeParam(params, "endDate");
        
        log.info("Generating user activity report (POST): startDate={}, endDate={}", startDate, endDate);
        
        return generateUserActivityReportResponse(startDate, endDate);
    }
    
    private ResponseEntity<InputStreamResource> generateUserActivityReportResponse(
            LocalDateTime startDate, LocalDateTime endDate) {
        
        ByteArrayInputStream bis = reportService.generateUserActivityReport(startDate, endDate);
        
        String filename = "User_Activity_Report_" + LocalDateTime.now().format(DATE_FORMATTER) + ".pdf";
        return generateResponseFromInputStream(bis, filename);
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getReportStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Fetching report statistics: startDate={}, endDate={}", startDate, endDate);
        
        Map<String, Object> statistics = reportService.getReportStatistics(startDate, endDate);
        
        return ResponseEntity.ok(statistics);
    }
    
    // Helper method to generate report response
    private ResponseEntity<InputStreamResource> generateResponseFromInputStream(
            ByteArrayInputStream bis, String filename) {
        
        // If bis is empty or null, return 204 No Content with appropriate headers
        if (bis == null) {
            log.info("No content available (null ByteArrayInputStream) for report: {}", filename);
            return ResponseEntity
                    .noContent()
                    .build();
        }
        
        // Check if the stream is empty
        int available;
        try {
            available = bis.available();
            if (available == 0) {
                log.info("No content available (empty ByteArrayInputStream) for report: {}", filename);
                return ResponseEntity
                        .noContent()
                        .build();
            }
            
            // Reset stream position after checking
            bis.reset();
            
            log.info("Generating response with PDF content, filename: {}, content size: {} bytes", filename, available);
            
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(available))
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(new InputStreamResource(bis));
        } catch (Exception e) {
            log.error("Error generating response for report: {}", filename, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    // Support both GET and POST for system overview report
    @GetMapping("/system-overview")
    public ResponseEntity<InputStreamResource> getSystemOverviewReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Generating system overview report (GET): startDate={}, endDate={}", startDate, endDate);
        
        ByteArrayInputStream bis = reportService.generateSystemOverviewReport(startDate, endDate);
        
        String filename = "System_Overview_Report_" + LocalDateTime.now().format(DATE_FORMATTER) + ".pdf";
        return generateResponseFromInputStream(bis, filename);
    }
    
    @PostMapping("/system-overview")
    public ResponseEntity<InputStreamResource> postSystemOverviewReport(
            @RequestBody(required = false) Map<String, Object> params) {
        
        LocalDateTime startDate = getDateTimeParam(params, "startDate");
        LocalDateTime endDate = getDateTimeParam(params, "endDate");
        
        log.info("Generating system overview report (POST): startDate={}, endDate={}", startDate, endDate);
        
        ByteArrayInputStream bis = reportService.generateSystemOverviewReport(startDate, endDate);
        
        String filename = "System_Overview_Report_" + LocalDateTime.now().format(DATE_FORMATTER) + ".pdf";
        return generateResponseFromInputStream(bis, filename);
    }
    
    // Helper methods for parsing parameters
    private String getStringParam(Map<String, Object> params, String paramName) {
        if (params == null || !params.containsKey(paramName)) {
            return null;
        }
        Object value = params.get(paramName);
        return value != null ? value.toString() : null;
    }
    
    private Long getLongParam(Map<String, Object> params, String paramName) {
        if (params == null || !params.containsKey(paramName)) {
            return null;
        }
        Object value = params.get(paramName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private LocalDateTime getDateTimeParam(Map<String, Object> params, String paramName) {
        if (params == null || !params.containsKey(paramName)) {
            return null;
        }
        Object value = params.get(paramName);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            try {
                return LocalDateTime.parse((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, String>> handleStoredProcedureErrors(InvalidDataAccessApiUsageException ex) {
        log.error("Stored procedure error: {}", ex.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "Database Error");
        response.put("message", "A database stored procedure could not be executed. Please ensure all required database migrations have been run.");
        response.put("details", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsRequest() {
        log.info("OPTIONS request received for reports endpoint");
        return ResponseEntity.ok().build();
    }
} 