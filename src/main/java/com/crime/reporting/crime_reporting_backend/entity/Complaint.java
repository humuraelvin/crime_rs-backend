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
@Table(name = "complaints")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrimeType crimeType;
    
    @Column(nullable = false, length = 2000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;
    
    @Column(nullable = false)
    private LocalDateTime dateFiled;
    
    @Column
    private LocalDateTime dateLastUpdated;
    
    @Column
    private String location;
    
    @Column
    private Integer priorityScore;
    
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL)
    private List<Evidence> evidences;
    
    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL)
    private CaseFile caseFile;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private PoliceOfficer assignedOfficer;
    
    @PrePersist
    protected void onCreate() {
        dateFiled = LocalDateTime.now();
        dateLastUpdated = LocalDateTime.now();
        if (status == null) {
            status = ComplaintStatus.SUBMITTED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        dateLastUpdated = LocalDateTime.now();
    }
}
