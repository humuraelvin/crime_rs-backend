package com.crime.reporting.crime_reporting_backend.repository;

import com.crime.reporting.crime_reporting_backend.entity.Role;
import com.crime.reporting.crime_reporting_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Role-based queries
    List<User> findByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRole(Role role);
    
    // User statistics
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND MONTH(u.createdAt) = MONTH(CURRENT_DATE) AND YEAR(u.createdAt) = YEAR(CURRENT_DATE)")
    long countNewUsersByRoleThisMonth(Role role);
    
    @Query("SELECT MONTH(u.createdAt), COUNT(u) FROM User u WHERE YEAR(u.createdAt) = YEAR(CURRENT_DATE) GROUP BY MONTH(u.createdAt)")
    List<Object[]> countUsersByMonth();
} 