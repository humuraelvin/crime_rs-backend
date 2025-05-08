package com.crime.reporting.crime_reporting_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StoredProcedureRepository {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private SimpleJdbcCall complaintStatsByDateRangeProc;
    private SimpleJdbcCall complaintStatsByCrimeTypeProc;
    private SimpleJdbcCall userActivityReportProc;
    private SimpleJdbcCall complaintReportProc;
    private SimpleJdbcCall officerPerformanceReportProc;
    private SimpleJdbcCall systemOverviewStatsProc;

    public StoredProcedureRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        // Initialize SimpleJdbcCall objects
        this.complaintStatsByDateRangeProc = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("get_complaint_stats_by_date_range");
                
        this.complaintStatsByCrimeTypeProc = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("get_complaint_stats_by_crime_type");
                
        this.userActivityReportProc = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("get_user_activity_report");
                
        this.complaintReportProc = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("get_complaint_report");
                
        this.officerPerformanceReportProc = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("get_officer_performance_report");

        this.systemOverviewStatsProc = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("get_system_overview_stats");
    }

    /**
     * Get complaint statistics by date range using stored procedure
     */
    public Map<String, Object> getComplaintStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("start_date", startDate);
            params.put("end_date", endDate);
            
            return complaintStatsByDateRangeProc.execute(params);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StoredProcedureRepository.class)
                .error("Error executing get_complaint_stats_by_date_range: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("results", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * Get complaint statistics by crime type using stored procedure
     */
    public Map<String, Object> getComplaintStatsByCrimeType() {
        try {
            return complaintStatsByCrimeTypeProc.execute();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StoredProcedureRepository.class)
                .error("Error executing get_complaint_stats_by_crime_type: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("results", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * Get user activity report for admin dashboard
     */
    public Map<String, Object> getUserActivityReport(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("start_date", startDate);
            params.put("end_date", endDate);
            
            return userActivityReportProc.execute(params);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StoredProcedureRepository.class)
                .error("Error executing get_user_activity_report: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("results", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * Get complaint report with filter parameters
     */
    public Map<String, Object> getComplaintReport(LocalDateTime startDate, LocalDateTime endDate, 
                                             String status, String crimeType) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("start_date", startDate);
            params.put("end_date", endDate);
            params.put("status", status);
            params.put("crime_type", crimeType);
            
            return complaintReportProc.execute(params);
        } catch (Exception e) {
            // Log the error and return an empty result
            org.slf4j.LoggerFactory.getLogger(StoredProcedureRepository.class)
                .error("Error executing get_complaint_report procedure: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("results", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * Get officer performance report for admin dashboard
     */
    public Map<String, Object> getOfficerPerformanceReport(Long departmentId, LocalDateTime startDate, 
                                                     LocalDateTime endDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("department_id", departmentId);
            params.put("start_date", startDate);
            params.put("end_date", endDate);
            
            return officerPerformanceReportProc.execute(params);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StoredProcedureRepository.class)
                .error("Error executing get_officer_performance_report: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("results", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * Get system overview statistics
     */
    public Map<String, Object> getSystemOverviewStats(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("start_date", startDate);
            params.put("end_date", endDate);
            
            return systemOverviewStatsProc.execute(params);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StoredProcedureRepository.class)
                .error("Error executing get_system_overview_stats: {}", e.getMessage(), e);
            
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("results", new java.util.ArrayList<>());
            return emptyResult;
        }
    }

    /**
     * Execute custom SQL query with parameters
     */
    public List<Map<String, Object>> executeQuery(String sql, Map<String, Object> params) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (params != null) {
            params.forEach(parameters::addValue);
        }
        
        return namedParameterJdbcTemplate.queryForList(sql, parameters);
    }
} 