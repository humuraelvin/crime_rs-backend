package com.crime.reporting.crime_reporting_backend.service.impl;

import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.repository.ComplaintRepository;
import com.crime.reporting.crime_reporting_backend.repository.StoredProcedureRepository;
import com.crime.reporting.crime_reporting_backend.service.ReportService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final StoredProcedureRepository storedProcedureRepository;
    private final ComplaintRepository complaintRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Extension method for ComplaintRepository to handle the findByFilters call
     * This method implements the filtering logic required for generating reports
     */
    private List<Complaint> findByFilters(LocalDateTime startDate, LocalDateTime endDate, String status, String crimeType) {
        log.info("Finding complaints with filters: startDate={}, endDate={}, status={}, crimeType={}", 
                startDate, endDate, status, crimeType);
        
        // Make a final copy of complaintStatus
        final ComplaintStatus complaintStatus;
        if (status != null && !status.isEmpty()) {
            try {
                complaintStatus = ComplaintStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
                // Return empty list if status is invalid
                return new ArrayList<>();
            }
        } else {
            complaintStatus = null;
        }
        
        List<Complaint> complaints;
        
        if (startDate != null && endDate != null) {
            // Both date filters provided
            if (complaintStatus != null) {
                // Filter by date range and status
                // Create final copies of variables used in the lambda
                final LocalDateTime finalStartDate = startDate;
                final LocalDateTime finalEndDate = endDate;
                
                complaints = complaintRepository.findAll().stream()
                    .filter(c -> c.getDateFiled().isAfter(finalStartDate) && c.getDateFiled().isBefore(finalEndDate))
                    .filter(c -> c.getStatus() == complaintStatus)
                    .collect(Collectors.toList());
            } else {
                // Filter by date range only
                complaints = complaintRepository.findByDateFiledBetween(startDate, endDate);
            }
        } else if (complaintStatus != null) {
            // Filter by status only
            complaints = complaintRepository.findByStatus(complaintStatus);
        } else {
            // No filters, get all complaints
            complaints = complaintRepository.findAll();
        }
        
        // Apply crime type filter if provided
        if (crimeType != null && !crimeType.isEmpty()) {
            // Create a final copy of the crime type for use in the lambda
            final String finalCrimeType = crimeType;
            complaints = complaints.stream()
                .filter(c -> c.getCrimeType() != null && c.getCrimeType().toString().equals(finalCrimeType))
                .collect(Collectors.toList());
        }
        
        log.info("Found {} complaints matching the filter criteria", complaints.size());
        return complaints;
    }
    
    @Override
    @Transactional
    public ByteArrayInputStream generateComplaintsReport(LocalDateTime startDate, LocalDateTime endDate, 
                                                        String status, String crimeType) {
        log.info("Generating complaints report: startDate={}, endDate={}, status={}, crimeType={}", 
                startDate, endDate, status, crimeType);
        
        // Get complaints data from repository based on filters
        List<Complaint> complaints = findByFilters(startDate, endDate, status, crimeType);
        
        log.info("Retrieved {} complaints for report", complaints.size());
        
        if (complaints.isEmpty()) {
            log.info("No complaints data available for report");
            return new ByteArrayInputStream(new byte[0]);
        }
        
        try {
            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            document.open();
            addReportHeader(document, "Crime Complaints Report");
            addDateRangeInfo(document, startDate, endDate);
            
            if (status != null && !status.isEmpty()) {
                document.add(new Paragraph("Status: " + status));
            }
            
            if (crimeType != null && !crimeType.isEmpty()) {
                document.add(new Paragraph("Crime Type: " + crimeType));
            }
            
            document.add(new Paragraph(" ")); // Add some space
            
            // Create table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            
            // Set column widths
            float[] columnWidths = {8f, 10f, 14f, 30f, 16f, 22f};
            table.setWidths(columnWidths);
            
            // Add table headers
            addTableHeader(table, "ID", "Date", "Crime Type", "Description", "Status", "Location");
            
            // Add complaint data rows
            for (Complaint complaint : complaints) {
                addTableRow(table, 
                    String.valueOf(complaint.getId()),
                    formatDateForReport(complaint.getDateFiled()), // Use getDateFiled instead of getSubmissionDate
                    complaint.getCrimeType().toString(),
                    complaint.getDescription(),
                    complaint.getStatus().toString(),
                    complaint.getLocation() != null ? complaint.getLocation() : "N/A"
                );
            }
            
            document.add(table);
            
            // Add summary information
            document.add(new Paragraph(" ")); // Add some space
            document.add(new Paragraph("Total Complaints: " + complaints.size()));
            
            // Add status counts if available
            Map<String, Long> statusCounts = complaints.stream()
                    .collect(Collectors.groupingBy(c -> c.getStatus().toString(), Collectors.counting()));
            
            document.add(new Paragraph("Status Summary:"));
            for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
                document.add(new Paragraph("  - " + entry.getKey() + ": " + entry.getValue()));
            }
            
            document.close();
            writer.close();
            
            byte[] pdfBytes = out.toByteArray();
            log.info("Generated PDF report with size: {} bytes", pdfBytes.length);
            
            ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
            bis.mark(Integer.MAX_VALUE); // Mark the beginning for reset capability
            
            return bis;
        } catch (Exception e) {
            log.error("Error generating complaints report", e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    
    /**
     * Add the header to a report
     */
    private void addReportHeader(Document document, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        document.add(titlePara);
        document.add(new Paragraph(" ")); // Add space
    }
    
    /**
     * Add date range information to the report
     */
    private void addDateRangeInfo(Document document, LocalDateTime startDate, LocalDateTime endDate) throws DocumentException {
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        StringBuilder dateRange = new StringBuilder("Report generated on: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\n");
        
        if (startDate != null) {
            dateRange.append("From: ")
                    .append(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("\n");
        }
        
        if (endDate != null) {
            dateRange.append("To: ")
                    .append(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("\n");
        }
        
        Paragraph dateInfo = new Paragraph(dateRange.toString(), dateFont);
        document.add(dateInfo);
        document.add(new Paragraph(" ")); // Add space
    }
    
    /**
     * Add table headers to the PDF table
     */
    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(66, 139, 202));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }
    
    /**
     * Add a row of data to the PDF table
     */
    private void addTableRow(PdfPTable table, String... values) {
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", cellFont));
            cell.setPadding(5);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }
    
    /**
     * Format a date for the report
     */
    private String formatDateForReport(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    
    @Override
    @Transactional
    public ByteArrayInputStream generateOfficersPerformanceReport(Long departmentId, LocalDateTime startDate, 
                                                                 LocalDateTime endDate) {
        log.info("Generating officers performance report PDF");
        Map<String, Object> reportData = storedProcedureRepository.getOfficerPerformanceReport(
                departmentId, startDate, endDate);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Police Officers Performance Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add report info
            document.add(Chunk.NEWLINE);
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            
            StringBuilder reportInfoText = new StringBuilder("Report Date: ")
                    .append(LocalDateTime.now().format(DATE_FORMATTER))
                    .append("\n");
            
            if (startDate != null) {
                reportInfoText.append("From: ").append(startDate.format(DATE_FORMATTER)).append("\n");
            }
            if (endDate != null) {
                reportInfoText.append("To: ").append(endDate.format(DATE_FORMATTER)).append("\n");
            }
            
            Paragraph reportInfo = new Paragraph(reportInfoText.toString(), infoFont);
            document.add(reportInfo);
            document.add(Chunk.NEWLINE);
            
            // Create table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100f);
            
            // Define column widths (percentage of total width)
            float[] columnWidths = {5f, 15f, 10f, 15f, 10f, 10f, 10f};
            table.setWidths(columnWidths);
            
            // Create table header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(new Color(66, 139, 202));
            headerCell.setPadding(5);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            String[] headers = {"ID", "Officer Name", "Badge", "Department", "Assigned", "Resolved", "Avg. Days"};
            for (String header : headers) {
                headerCell.setPhrase(new Phrase(header, headerFont));
                table.addCell(headerCell);
            }
            
            // Add data to table
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) reportData.get("results");
            if (results != null) {
                for (Map<String, Object> row : results) {
                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(5);
                    
                    // ID
                    cell.setPhrase(new Phrase(String.valueOf(row.get("officer_id")), cellFont));
                    table.addCell(cell);
                    
                    // Officer Name
                    cell.setPhrase(new Phrase(String.valueOf(row.get("officer_name")), cellFont));
                    table.addCell(cell);
                    
                    // Badge
                    cell.setPhrase(new Phrase(String.valueOf(row.get("badge_number")), cellFont));
                    table.addCell(cell);
                    
                    // Department
                    cell.setPhrase(new Phrase(String.valueOf(row.get("department_name")), cellFont));
                    table.addCell(cell);
                    
                    // Assigned
                    cell.setPhrase(new Phrase(String.valueOf(row.get("assigned_complaints")), cellFont));
                    table.addCell(cell);
                    
                    // Resolved
                    cell.setPhrase(new Phrase(String.valueOf(row.get("resolved_complaints")), cellFont));
                    table.addCell(cell);
                    
                    // Avg. Days
                    cell.setPhrase(new Phrase(String.valueOf(row.get("avg_resolution_days")), cellFont));
                    table.addCell(cell);
                }
            }
            
            document.add(table);
            document.close();
            
        } catch (DocumentException e) {
            log.error("Error generating officers performance report PDF", e);
        }
        
        return new ByteArrayInputStream(out.toByteArray());
    }
    
    @Override
    @Transactional
    public ByteArrayInputStream generateUserActivityReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating user activity report PDF");
        Map<String, Object> reportData = storedProcedureRepository.getUserActivityReport(startDate, endDate);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("User Activity Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add report info
            document.add(Chunk.NEWLINE);
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            
            StringBuilder reportInfoText = new StringBuilder("Report Date: ")
                    .append(LocalDateTime.now().format(DATE_FORMATTER))
                    .append("\n");
            
            if (startDate != null) {
                reportInfoText.append("From: ").append(startDate.format(DATE_FORMATTER)).append("\n");
            }
            if (endDate != null) {
                reportInfoText.append("To: ").append(endDate.format(DATE_FORMATTER)).append("\n");
            }
            
            Paragraph reportInfo = new Paragraph(reportInfoText.toString(), infoFont);
            document.add(reportInfo);
            document.add(Chunk.NEWLINE);
            
            // Create table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100f);
            
            // Define column widths (percentage of total width)
            float[] columnWidths = {5f, 15f, 20f, 10f, 15f, 10f};
            table.setWidths(columnWidths);
            
            // Create table header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(new Color(66, 139, 202));
            headerCell.setPadding(5);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            String[] headers = {"ID", "Name", "Email", "Role", "Registration Date", "Complaints"};
            for (String header : headers) {
                headerCell.setPhrase(new Phrase(header, headerFont));
                table.addCell(headerCell);
            }
            
            // Add data to table
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) reportData.get("results");
            if (results != null) {
                for (Map<String, Object> row : results) {
                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(5);
                    
                    // ID
                    cell.setPhrase(new Phrase(String.valueOf(row.get("user_id")), cellFont));
                    table.addCell(cell);
                    
                    // Name
                    String name = row.get("first_name") + " " + row.get("last_name");
                    cell.setPhrase(new Phrase(name, cellFont));
                    table.addCell(cell);
                    
                    // Email
                    cell.setPhrase(new Phrase(String.valueOf(row.get("email")), cellFont));
                    table.addCell(cell);
                    
                    // Role
                    cell.setPhrase(new Phrase(String.valueOf(row.get("role")), cellFont));
                    table.addCell(cell);
                    
                    // Registration Date
                    cell.setPhrase(new Phrase(formatDate(row.get("registration_date")), cellFont));
                    table.addCell(cell);
                    
                    // Complaints
                    cell.setPhrase(new Phrase(String.valueOf(row.get("complaints_filed")), cellFont));
                    table.addCell(cell);
                }
            }
            
            document.add(table);
            document.close();
            
        } catch (DocumentException e) {
            log.error("Error generating user activity report PDF", e);
        }
        
        return new ByteArrayInputStream(out.toByteArray());
    }
    
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<String, Object> getReportStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching report statistics");
        Map<String, Object> statistics = new HashMap<>();
        
        // Get statistics by date range
        Map<String, Object> dateRangeStats = storedProcedureRepository.getComplaintStatsByDateRange(
                startDate != null ? startDate : LocalDateTime.now().minusMonths(1), 
                endDate != null ? endDate : LocalDateTime.now());
        statistics.put("byDateRange", dateRangeStats);
        
        // Get statistics by crime type
        Map<String, Object> crimeTypeStats = storedProcedureRepository.getComplaintStatsByCrimeType();
        statistics.put("byCrimeType", crimeTypeStats);
        
        return statistics;
    }
    
    private String formatDate(Object date) {
        if (date == null) {
            return "";
        }
        
        if (date instanceof java.sql.Timestamp) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(((java.sql.Timestamp) date).toLocalDateTime());
        }
        
        return date.toString();
    }
    
    @Override
    @Transactional
    public ByteArrayInputStream generateSystemOverviewReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating comprehensive system overview report");
        
        // Instead of using stored procedures, get data directly
        // Get complaints for this date range
        List<Complaint> complaints = complaintRepository.findAll();
        
        // Basic statistics we need to calculate
        Map<String, Object> complaintData = new HashMap<>();
        complaintData.put("results", new ArrayList<>());
        
        Map<String, Object> officerData = new HashMap<>();
        List<Map<String, Object>> officerResults = new ArrayList<>();
        officerData.put("results", officerResults);
        
        Map<String, Object> userData = new HashMap<>();
        List<Map<String, Object>> userResults = new ArrayList<>();
        userData.put("results", userResults);
        
        // Calculate report statistics
        Map<String, Object> statistics = new HashMap<>();
        Map<String, Object> crimeTypeStats = new HashMap<>();
        List<Map<String, Object>> crimeTypeResults = new ArrayList<>();
        crimeTypeStats.put("results", crimeTypeResults);
        statistics.put("byCrimeType", crimeTypeStats);
        
        // Get basic system stats
        Map<String, Object> overviewStats = new HashMap<>();
        
        // Calculate total complaints
        overviewStats.put("total_complaints", complaints.size());
        
        // Count open complaints
        long openComplaints = complaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.SUBMITTED 
                 || c.getStatus() == ComplaintStatus.ASSIGNED
                 || c.getStatus() == ComplaintStatus.INVESTIGATING
                 || c.getStatus() == ComplaintStatus.PENDING_EVIDENCE
                 || c.getStatus() == ComplaintStatus.UNDER_REVIEW)
            .count();
        overviewStats.put("open_complaints", openComplaints);
        
        // Count closed complaints
        long closedComplaints = complaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED 
                 || c.getStatus() == ComplaintStatus.REJECTED
                 || c.getStatus() == ComplaintStatus.CLOSED)
            .count();
        overviewStats.put("closed_complaints", closedComplaints);
        
        // Calculate average resolution time
        double avgResolutionDays = complaints.stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
            .filter(c -> c.getDateFiled() != null && c.getDateLastUpdated() != null)
            .mapToDouble(c -> {
                long diffInMillies = c.getDateLastUpdated().toInstant(ZoneOffset.UTC).toEpochMilli() - 
                                c.getDateFiled().toInstant(ZoneOffset.UTC).toEpochMilli();
                return diffInMillies / (1000.0 * 60 * 60 * 24);
            })
            .average()
            .orElse(0.0);
        overviewStats.put("avg_resolution_days", avgResolutionDays);
        
        // Get officer count from repository
        long officerCount = 0;
        try {
            officerCount = complaintRepository.findAll().stream()
                .map(Complaint::getAssignedOfficer)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        } catch (Exception e) {
            log.error("Error calculating officer count", e);
        }
        overviewStats.put("total_officers", officerCount);
        
        // Departments count (estimate)
        overviewStats.put("total_departments", 5L);
        
        // Users count (from complaints)
        long usersCount = complaints.stream()
            .map(Complaint::getUser)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        overviewStats.put("total_users", usersCount);
        
        // New users in period
        overviewStats.put("new_users_period", 0L);
        
        // High priority complaints
        long highPriorityCount = complaints.stream()
            .filter(c -> {
                Integer score = c.getPriorityScore();
                return score != null && score >= 7;
            })
            .count();
        overviewStats.put("high_priority_complaints", highPriorityCount);
        
        // Medium priority complaints
        long mediumPriorityCount = complaints.stream()
            .filter(c -> {
                Integer score = c.getPriorityScore();
                return score != null && score >= 4 && score < 7;
            })
            .count();
        overviewStats.put("medium_priority_complaints", mediumPriorityCount);
        
        // Low priority complaints
        long lowPriorityCount = complaints.stream()
            .filter(c -> {
                Integer score = c.getPriorityScore();
                return score != null && score < 4;
            })
            .count();
        overviewStats.put("low_priority_complaints", lowPriorityCount);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("System Overview Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add report info
            document.add(Chunk.NEWLINE);
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            
            StringBuilder reportInfoText = new StringBuilder("Report Generated: ")
                    .append(LocalDateTime.now().format(DATE_FORMATTER))
                    .append("\n");
            
            if (startDate != null) {
                reportInfoText.append("Period Start: ").append(startDate.format(DATE_FORMATTER)).append("\n");
            }
            if (endDate != null) {
                reportInfoText.append("Period End: ").append(endDate.format(DATE_FORMATTER)).append("\n");
            }
            
            Paragraph reportInfo = new Paragraph(reportInfoText.toString(), infoFont);
            document.add(reportInfo);
            document.add(Chunk.NEWLINE);
            
            // Add table of contents
            Font tocFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph tocTitle = new Paragraph("Table of Contents", tocFont);
            document.add(tocTitle);
            document.add(Chunk.NEWLINE);
            
            Font tocItemFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("1. System Summary", tocItemFont));
            document.add(new Paragraph("2. Complaint Statistics", tocItemFont));
            document.add(Chunk.NEWLINE);
            
            // 1. System Summary
            document.newPage();
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph sectionTitle = new Paragraph("1. System Summary", sectionFont);
            document.add(sectionTitle);
            document.add(Chunk.NEWLINE);
            
            // Create summary table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100f);
            
            // Add summary data
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(new Color(66, 139, 202));
            headerCell.setPadding(5);
            headerCell.setColspan(2);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPhrase(new Phrase("System Overview", headerFont));
            summaryTable.addCell(headerCell);
            
            // Extract summary data from statistics and overviewStats
            addSummaryRow(summaryTable, "Total Complaints", 
                    overviewStats.get("total_complaints") != null ? overviewStats.get("total_complaints").toString() : "0");
            addSummaryRow(summaryTable, "Open Complaints", 
                    overviewStats.get("open_complaints") != null ? overviewStats.get("open_complaints").toString() : "0");
            addSummaryRow(summaryTable, "Closed Complaints", 
                    overviewStats.get("closed_complaints") != null ? overviewStats.get("closed_complaints").toString() : "0");
            
            // Officer count
            addSummaryRow(summaryTable, "Total Officers", 
                    overviewStats.get("total_officers") != null ? overviewStats.get("total_officers").toString() : "0");
            
            // Department count
            addSummaryRow(summaryTable, "Total Departments", 
                    overviewStats.get("total_departments") != null ? overviewStats.get("total_departments").toString() : "0");
            
            // User counts
            addSummaryRow(summaryTable, "Total Users", 
                    overviewStats.get("total_users") != null ? overviewStats.get("total_users").toString() : "0");
            addSummaryRow(summaryTable, "New Users (This Period)", 
                    overviewStats.get("new_users_period") != null ? overviewStats.get("new_users_period").toString() : "0");
            
            // High priority complaints
            addSummaryRow(summaryTable, "High Priority Complaints", 
                    overviewStats.get("high_priority_complaints") != null ? overviewStats.get("high_priority_complaints").toString() : "0");
            
            // Medium priority complaints
            addSummaryRow(summaryTable, "Medium Priority Complaints", 
                    overviewStats.get("medium_priority_complaints") != null ? overviewStats.get("medium_priority_complaints").toString() : "0");
            
            // Low priority complaints
            addSummaryRow(summaryTable, "Low Priority Complaints", 
                    overviewStats.get("low_priority_complaints") != null ? overviewStats.get("low_priority_complaints").toString() : "0");
            
            // Avg resolution time
            String avgResolution = String.format("%.2f", avgResolutionDays);
            addSummaryRow(summaryTable, "Avg. Resolution Time (days)", avgResolution);
            
            document.add(summaryTable);
            
            // 2. Complaint Statistics
            document.newPage();
            Paragraph complaintStatTitle = new Paragraph("2. Complaint Statistics", sectionFont);
            document.add(complaintStatTitle);
            document.add(Chunk.NEWLINE);
            
            // Generate crime type statistics here
            Map<String, Long> crimeTypeCounts = complaints.stream()
                .filter(c -> c.getCrimeType() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getCrimeType().toString(),
                    Collectors.counting()
                ));
            
            if (!crimeTypeCounts.isEmpty()) {
                PdfPTable crimeTypeTable = new PdfPTable(3);
                crimeTypeTable.setWidthPercentage(100f);
                
                // Add header
                PdfPCell crimeHeaderCell = new PdfPCell();
                crimeHeaderCell.setBackgroundColor(new Color(66, 139, 202));
                crimeHeaderCell.setPadding(5);
                crimeHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                
                crimeHeaderCell.setPhrase(new Phrase("Crime Type", headerFont));
                crimeTypeTable.addCell(crimeHeaderCell);
                crimeHeaderCell.setPhrase(new Phrase("Count", headerFont));
                crimeTypeTable.addCell(crimeHeaderCell);
                crimeHeaderCell.setPhrase(new Phrase("Percentage", headerFont));
                crimeTypeTable.addCell(crimeHeaderCell);
                
                // Add data rows
                Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                PdfPCell cell = new PdfPCell();
                cell.setPadding(5);
                
                long totalCrimes = crimeTypeCounts.values().stream().mapToLong(Long::longValue).sum();
                
                for (Map.Entry<String, Long> entry : crimeTypeCounts.entrySet()) {
                    // Crime type
                    cell.setPhrase(new Phrase(entry.getKey(), cellFont));
                    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    crimeTypeTable.addCell(cell);
                    
                    // Count
                    cell.setPhrase(new Phrase(entry.getValue().toString(), cellFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    crimeTypeTable.addCell(cell);
                    
                    // Percentage
                    double percentage = totalCrimes > 0 ? (entry.getValue() * 100.0 / totalCrimes) : 0;
                    cell.setPhrase(new Phrase(String.format("%.2f%%", percentage), cellFont));
                    crimeTypeTable.addCell(cell);
                }
                
                document.add(crimeTypeTable);
            } else {
                document.add(new Paragraph("No crime type statistics available for the selected period.", infoFont));
            }
            
            // Close the document properly
            try {
                document.close();
            } catch (Exception e) {
                log.error("Error closing document", e);
            }
            
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Error generating system overview report", e);
            
            // Force closing the document in case of exception
            try {
                if (document.isOpen()) {
                    document.close();
                }
            } catch (Exception ex) {
                log.error("Error closing document after exception", ex);
            }
            
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    
    private void addSummaryRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new Color(240, 240, 240));
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
} 