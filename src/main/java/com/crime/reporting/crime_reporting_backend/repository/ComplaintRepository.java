package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.ComplaintStatus;
import com.crime.reporting.crime_reporting_backend.entity.CrimeType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUser(User user);
    Page<Complaint> findByUser(User user, Pageable pageable);
    List<Complaint> findByStatus(ComplaintStatus status);
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    List<Complaint> findByCrimeType(CrimeType crimeType);
    
    @Query("SELECT c FROM Complaint c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:crimeType IS NULL OR c.crimeType = :crimeType) AND " +
            "(:startDate IS NULL OR c.dateFiled >= :startDate) AND " +
            "(:endDate IS NULL OR c.dateFiled <= :endDate)")
    Page<Complaint> findComplaintsWithFilters(
            ComplaintStatus status,
            CrimeType crimeType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
    
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.status = :status")
    long countByStatus(ComplaintStatus status);
    
    @Query("SELECT c.crimeType, COUNT(c) FROM Complaint c GROUP BY c.crimeType ORDER BY COUNT(c) DESC")
    List<Object[]> countByCrimeType();
    
    @Query("SELECT FUNCTION('DATE', c.dateFiled) as date, COUNT(c) FROM Complaint c " +
            "WHERE c.dateFiled BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', c.dateFiled) ORDER BY date")
    List<Object[]> countByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
} 