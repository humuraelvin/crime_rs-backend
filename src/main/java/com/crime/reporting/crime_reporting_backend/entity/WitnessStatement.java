package com.crime.reporting.crime_reporting_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "witness_statements")
public class WitnessStatement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CaseFile caseFile;
    
    @Column(nullable = false)
    private String witnessName;
    
    @Column
    private String witnessContact;
    
    @Column(nullable = false, length = 3000)
    private String statement;
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @Column
    private Boolean isAnonymous;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (isAnonymous == null) {
            isAnonymous = false;
        }
    }
} 