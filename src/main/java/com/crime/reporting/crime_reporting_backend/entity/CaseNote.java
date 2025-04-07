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
@Table(name = "case_notes")
public class CaseNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CaseFile caseFile;
    
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @Column(nullable = false, length = 2000)
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private Boolean isPrivate;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isPrivate == null) {
            isPrivate = true;
        }
    }
} 