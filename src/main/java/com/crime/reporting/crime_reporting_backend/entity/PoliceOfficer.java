package com.crime.reporting.crime_reporting_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    @Column(nullable = false, unique = true)
    private String badgeNumber;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    // Alternative approach - direct ID field
    @Transient // Not persisted, just for convenience
    private Long departmentDirectId;
    
    @Column(nullable = false)
    private String rank;
    
    @Column
    private String specialization;
    
    @Column
    private String contactInfo;
    
    @Column
    private String jurisdiction;
    
    @OneToMany(mappedBy = "assignedOfficer")
    @Builder.Default
    private List<CaseFile> caseFiles = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Get the department ID directly
    public Long getDepartmentId() {
        return department != null ? department.getId() : departmentDirectId;
    }
    
    @Override
    public String toString() {
        return "PoliceOfficer{" +
                "id=" + id +
                ", badgeNumber='" + badgeNumber + '\'' +
                ", departmentId=" + getDepartmentId() +
                ", rank='" + rank + '\'' +
                '}';
    }
}
