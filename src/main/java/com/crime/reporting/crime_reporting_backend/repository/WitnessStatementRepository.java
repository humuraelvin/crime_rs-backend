package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.CaseFile;
import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.entity.WitnessStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WitnessStatementRepository extends JpaRepository<WitnessStatement, Long> {
    List<WitnessStatement> findByCaseFile(CaseFile caseFile);
    Page<WitnessStatement> findByCaseFile(CaseFile caseFile, Pageable pageable);
    List<WitnessStatement> findByUser(User user);
    List<WitnessStatement> findByWitnessName(String witnessName);
    List<WitnessStatement> findByIsAnonymous(Boolean isAnonymous);
} 