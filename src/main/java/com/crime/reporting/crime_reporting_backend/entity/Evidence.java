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
@Table(name = "evidences")
public class Evidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EvidenceType evidenceType;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileUrl;
    
    @Column
    private String originalFileName;
    
    @Column
    private String fileType;
    
    @Column
    private Long fileSize;
    
    @Column(length = 2000)
    private String description;
    
    @Column
    private String metadata;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadDate;
    
    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
    
    @PrePersist
    protected void onCreate() {
        if (uploadDate == null) {
            uploadDate = LocalDateTime.now();
        }
        if (evidenceType == null) {
            evidenceType = EvidenceType.OTHER;
        }
    }
}
