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
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role " +
           "AND FUNCTION('MONTH', u.createdAt) = FUNCTION('MONTH', CURRENT_DATE) " +
           "AND FUNCTION('YEAR', u.createdAt) = FUNCTION('YEAR', CURRENT_DATE)")
    long countNewUsersByRoleThisMonth(Role role);
    
    @Query("SELECT FUNCTION('MONTH', u.createdAt), COUNT(u) FROM User u " +
           "WHERE FUNCTION('YEAR', u.createdAt) = FUNCTION('YEAR', CURRENT_DATE) " +
           "GROUP BY FUNCTION('MONTH', u.createdAt)")
    List<Object[]> countUsersByMonth();
} 