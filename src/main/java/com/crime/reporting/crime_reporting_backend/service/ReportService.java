package com.crime.reporting.crime_reporting_backend.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.PageSize;

public interface ReportService {

    Logger log = LoggerFactory.getLogger(ReportService.class);

    /**
     * Generate PDF report for complaints based on filters
     */
    ByteArrayInputStream generateComplaintsReport(LocalDateTime startDate, LocalDateTime endDate, 
                                                 String status, String crimeType);
    
    /**
     * Generate PDF report for officer performance
     */
    ByteArrayInputStream generateOfficersPerformanceReport(Long departmentId, LocalDateTime startDate, 
                                                         LocalDateTime endDate);
    
    /**
     * Generate PDF report for user activity
     */
    ByteArrayInputStream generateUserActivityReport(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get statistics for reports
     */
    Map<String, Object> getReportStatistics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Generate comprehensive system overview report with all data
     */
    ByteArrayInputStream generateSystemOverviewReport(LocalDateTime startDate, LocalDateTime endDate);
} 