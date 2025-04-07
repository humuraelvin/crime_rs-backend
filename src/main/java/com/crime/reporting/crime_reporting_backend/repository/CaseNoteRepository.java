package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.CaseFile;
import com.crime.reporting.crime_reporting_backend.entity.CaseNote;
import com.crime.reporting.crime_reporting_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
    List<CaseNote> findByCaseFile(CaseFile caseFile);
    Page<CaseNote> findByCaseFile(CaseFile caseFile, Pageable pageable);
    List<CaseNote> findByAuthor(User author);
    List<CaseNote> findByCaseFileAndIsPrivate(CaseFile caseFile, Boolean isPrivate);
} 