package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.Complaint;
import com.crime.reporting.crime_reporting_backend.entity.Evidence;
import com.crime.reporting.crime_reporting_backend.entity.EvidenceType;
import com.crime.reporting.crime_reporting_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByComplaint(Complaint complaint);
    List<Evidence> findByComplaintId(Long complaintId);
    List<Evidence> findByEvidenceType(EvidenceType evidenceType);
    List<Evidence> findByUploadedBy(User user);
    List<Evidence> findByFileType(String contentType);
} 