package com.crime.reporting.crime_reporting_backend.service;

import com.crime.reporting.crime_reporting_backend.entity.User;
import com.crime.reporting.crime_reporting_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Enables MFA for a user
     * @param email the user's email
     * @param secret the MFA secret
     */
    @Transactional
    public void enableMfa(String email, String secret) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        userRepository.save(user);
    }
    
    /**
     * Disables MFA for a user
     * @param email the user's email
     */
    @Transactional
    public void disableMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }
} 