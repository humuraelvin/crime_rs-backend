package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoliceOfficerRepository extends JpaRepository<PoliceOfficer, Long> {
    Optional<PoliceOfficer> findByUser(User user);
    Optional<PoliceOfficer> findByBadgeNumber(String badgeNumber);
    List<PoliceOfficer> findByDepartment(String department);
    List<PoliceOfficer> findByRank(String rank);
    List<PoliceOfficer> findBySpecialization(String specialization);
    List<PoliceOfficer> findByJurisdiction(String jurisdiction);
} 