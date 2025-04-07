package com.crime.reporting.crime_reporting_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "case_files")
public class CaseFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "complaint_id", nullable = false, unique = true)
    private Complaint complaint;
    
    @ManyToOne
    @JoinColumn(name = "officer_id")
    private PoliceOfficer assignedOfficer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;
    
    @Column(length = 5000)
    private String reportSummary;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastUpdated;
    
    @Column
    private LocalDateTime closedAt;
    
    @OneToMany(mappedBy = "caseFile", cascade = CascadeType.ALL)
    private List<CaseNote> caseNotes;
    
    @OneToMany(mappedBy = "caseFile", cascade = CascadeType.ALL)
    private List<WitnessStatement> witnessStatements;
    
    @Column
    private String closingReport;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        if (status == null) {
            status = CaseStatus.OPEN;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        if (status == CaseStatus.CLOSED && closedAt == null) {
            closedAt = LocalDateTime.now();
        }
    }
}
