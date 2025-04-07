package com.crime.reporting.crime_reporting_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "police_officers")
public class PoliceOfficer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String badgeNumber;
    
    @Column(nullable = false)
    private String department;
    
    @Column(nullable = false)
    private String rank;
    
    @Column
    private String specialization;
    
    @Column
    private String contactInfo;
    
    @Column
    private String jurisdiction;
    
    @OneToMany(mappedBy = "assignedOfficer")
    private List<CaseFile> caseFiles;
}
