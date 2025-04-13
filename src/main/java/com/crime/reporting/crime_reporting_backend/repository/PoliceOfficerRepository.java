package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.Department;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoliceOfficerRepository extends JpaRepository<PoliceOfficer, Long> {
    Optional<PoliceOfficer> findByUser(User user);
    Optional<PoliceOfficer> findByBadgeNumber(String badgeNumber);
    
    // Department related queries
    List<PoliceOfficer> findByDepartment(Department department);
    Page<PoliceOfficer> findByDepartment(Department department, Pageable pageable);
    int countByDepartment(Department department);
    
    // Other queries
    List<PoliceOfficer> findByRank(String rank);
    List<PoliceOfficer> findBySpecialization(String specialization);
    List<PoliceOfficer> findByJurisdiction(String jurisdiction);
} 