package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.CaseFile;
import com.crime.reporting.crime_reporting_backend.entity.CaseStatus;
import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {
    Optional<CaseFile> findByComplaint(Complaint complaint);
    List<CaseFile> findByAssignedOfficer(PoliceOfficer officer);
    Page<CaseFile> findByAssignedOfficer(PoliceOfficer officer, Pageable pageable);
    List<CaseFile> findByStatus(CaseStatus status);
    Page<CaseFile> findByStatus(CaseStatus status, Pageable pageable);
    
    @Query("SELECT c FROM CaseFile c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:officer IS NULL OR c.assignedOfficer = :officer) AND " +
            "(:startDate IS NULL OR c.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR c.createdAt <= :endDate)")
    Page<CaseFile> findCaseFilesWithFilters(
            CaseStatus status,
            PoliceOfficer officer,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
    
    @Query("SELECT COUNT(c) FROM CaseFile c WHERE c.status = :status")
    long countByStatus(CaseStatus status);
    
    @Query("SELECT AVG(DATEDIFF(c.closedAt, c.createdAt)) FROM CaseFile c WHERE c.status = 'CLOSED'")
    Double getAverageResolutionTime();
    
    @Query("SELECT c.assignedOfficer.id, COUNT(c) FROM CaseFile c GROUP BY c.assignedOfficer.id")
    List<Object[]> countCasesByOfficer();
} 