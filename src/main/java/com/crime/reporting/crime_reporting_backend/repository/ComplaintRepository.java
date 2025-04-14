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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUser(User user);
    Page<Complaint> findByUser(User user, Pageable pageable);
    List<Complaint> findByStatus(ComplaintStatus status);
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    List<Complaint> findByCrimeType(CrimeType crimeType);
    
    // Date-based queries
    List<Complaint> findByDateFiledAfter(LocalDateTime date);
    List<Complaint> findByDateFiledBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Status-based queries
    long countByStatusIn(Collection<ComplaintStatus> statuses);
    
    @Query(value = "SELECT * FROM complaints c WHERE " +
            "(:status IS NULL OR c.status = :status\\:\\:VARCHAR) AND " +
            "(:crimeType IS NULL OR c.crime_type = :crimeType\\:\\:VARCHAR) AND " +
            "(:startDate IS NULL OR c.date_filed >= :startDate\\:\\:TIMESTAMP) AND " +
            "(:endDate IS NULL OR c.date_filed <= :endDate\\:\\:TIMESTAMP)", 
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM complaints c WHERE " +
            "(:status IS NULL OR c.status = :status\\:\\:VARCHAR) AND " +
            "(:crimeType IS NULL OR c.crime_type = :crimeType\\:\\:VARCHAR) AND " +
            "(:startDate IS NULL OR c.date_filed >= :startDate\\:\\:TIMESTAMP) AND " +
            "(:endDate IS NULL OR c.date_filed <= :endDate\\:\\:TIMESTAMP)")
    Page<Complaint> findComplaintsWithFilters(
            String status,
            String crimeType,
            String startDate,
            String endDate,
            Pageable pageable
    );
    
    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.evidences WHERE c.id = :id")
    Optional<Complaint> findByIdWithEvidences(Long id);
    
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.status = :status")
    long countByStatus(ComplaintStatus status);
    
    @Query("SELECT c.crimeType, COUNT(c) FROM Complaint c GROUP BY c.crimeType ORDER BY COUNT(c) DESC")
    List<Object[]> countByCrimeType();
    
    @Query("SELECT FUNCTION('DATE', c.dateFiled) as date, COUNT(c) FROM Complaint c " +
            "WHERE c.dateFiled BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', c.dateFiled) ORDER BY date")
    List<Object[]> countByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT FUNCTION('MONTH', c.dateFiled), COUNT(c) FROM Complaint c " +
            "WHERE FUNCTION('YEAR', c.dateFiled) = FUNCTION('YEAR', CURRENT_DATE) " +
            "GROUP BY FUNCTION('MONTH', c.dateFiled)")
    List<Object[]> countByMonthForCurrentYear();
    
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.user.id = :userId")
    long countByUserId(Long userId);
    
    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.evidences WHERE c.user = :user")
    List<Complaint> findByUserWithEvidences(User user);
    
    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.evidences WHERE c.user = :user")
    Page<Complaint> findByUserWithEvidences(User user, Pageable pageable);
    
    Page<Complaint> findByAssignedOfficerId(Long officerId, Pageable pageable);
    
    List<Complaint> findByAssignedOfficerId(Long officerId);
    
    List<Complaint> findByUserId(Long userId);
} 